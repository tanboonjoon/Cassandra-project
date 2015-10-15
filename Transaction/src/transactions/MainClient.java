package transactions;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MainClient {
	private Cluster cluster;
	private static Session session;

	// function to connect to the cluster
	public void connect(String node) {
		cluster = Cluster.builder().addContactPoint(node).build();
		Metadata metadata = cluster.getMetadata();
		System.out.printf("Connected to cluster: %s\n",
				metadata.getClusterName());
		for (Host host : metadata.getAllHosts()) {
			System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n",
					host.getDatacenter(), host.getAddress(), host.getRack());
		}
		// Get a session from your cluster and store the reference to it.
		session = cluster.connect();
	}

	//Close cluster
	public void close() {
		cluster.close();
	}

	
	
	
	// Function to read the file and call transactions
	private int readFile(String keyspace, int node, int clientCount) {
		int count = 0;
		// Reading the file
		for (int i = 0; i < clientCount; i++) {
			// File name
			String filename = String.valueOf(i) + ".txt";
			
			BufferedReader br = null;
			Transaction transaction = new Transaction(session, keyspace, node);
			
			try {
				String sCurrentLine;

				br = new BufferedReader(new FileReader(filename));

				while ((sCurrentLine = br.readLine()) != null) {
					System.out.println(sCurrentLine);
					String[] words = sCurrentLine.split(",");

					if ((words[0]).equals("N")) {
						/*
						 * New order transaction - C_ID, W_ID, D_ID, M and
						 * OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY.
						 */
						int c_id = Integer.parseInt(words[1]);
						int w_id = Integer.parseInt(words[2]);
						int d_id = Integer.parseInt(words[3]);
						int m = Integer.parseInt(words[4]);
						// int[] ol_i_id = null, ol_supply_w_id = null,
						// ol_quantity = null;

						// read the items in the new order
						int j = 0;

						String localLine;
						Set<orderList> orderSet = new HashSet<orderList>();
						while ((localLine = br.readLine()) != null) {
							String[] localValues = localLine.split(",");
							/*
							 * ol_i_id[j] = Integer.parseInt(localValues[0]);
							 * ol_supply_w_id[j] = Integer
							 * .parseInt(localValues[1]); ol_quantity[j] =
							 * Integer.parseInt(localValues[2]);
							 */
							orderList orderlist = new orderList(
									Integer.parseInt(localValues[0]),
									Integer.parseInt(localValues[1]),
									Integer.parseInt(localValues[2]), 0, null);
							orderSet.add(orderlist);
							j++;
							if (j == m) {
								break;
							}
						}
						// transaction.newOrder(c_id, w_id, d_id, m, ol_i_id,
						// ol_supply_w_id, ol_quantity);
						transaction.newOrder(w_id, d_id, c_id, m, orderSet);
						count++;
					} else if ((words[0]).equals("P")) {
						// Payment transaction -> C_W_ID, C_D_ID, C_ID, PAYMENT.
						int w_id = Integer.parseInt(words[1]);
						int d_id = Integer.parseInt(words[2]);
						int c_id = Integer.parseInt(words[3]);
						double payment = Double.parseDouble(words[4]);
						transaction.payment(w_id, d_id, c_id, payment);
						// transaction.payment(Integer.parseInt(words[1]),
						// Integer.parseInt(words[2]),
						// Integer.parseInt(words[3]),
						// Integer.parseInt(words[4]));
						count++;
					} else if ((words[0]).equals("D")) {
						// Delivery transaction -> W_ID, CARRIER_ID
						transaction.delivery(Integer.parseInt(words[1]),
								Integer.parseInt(words[2]));
						count++;
					} else if ((words[0]).equals("O")) {
						// Order Status transaction -> C_W_ID, C_D_ID, C_ID
						transaction.printOrderStatus(
								Integer.parseInt(words[1]),
								Integer.parseInt(words[2]),
								Integer.parseInt(words[3]));
						count++;
					} else if ((words[0]).equals("S")) {
						// Stock level transaction -> W_ID, D_ID, T, L
						transaction.stockLevel(Integer.parseInt(words[1]),
								Integer.parseInt(words[2]),
								Integer.parseInt(words[3]),
								Integer.parseInt(words[4]));
						count++;
					} else if ((words[0]).equals("I")) {
						// Popular Item transaction -> W_ID, D_ID, L
						transaction.printPopularItem(
								Integer.parseInt(words[1]),
								Integer.parseInt(words[2]),
								Integer.parseInt(words[3]));
						count++;
					} else {
						System.out.println("Invalid transaction");
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return count;
	}

	public static void main(String[] args) {

		// if the number of arguments are less than 3, return error
		if (args.length != 3) {
			System.out.println("Please enter three parameters: X, Y and Z!");
			System.out
					.println("X: Database (D8 or D40); Y: Nodes(1 or 2) and Z = No. of clients (1 to 100)!");
			System.exit(-1);
		}

		// Validating the entered database
		if (!(args[0]).equals("D8") && !(args[0]).equals("D40")) {
			System.out.println("Value received:" + args[0]
					+ ".Incorrect value of database. Please choose D8 or D40");
			System.exit(-1);
		}

		// Validating the entered Nodes
		if (!(args[1]).equals("1") && !(args[1]).equals("2")) {
			System.out.println("Value received:" + args[1]
					+ ".Incorrect value of Nodes. Please choose 1 or 2");
			System.exit(-1);
		}

		// Validating the entered client number
		int clientCount = Integer.parseInt(args[2]);
		if (clientCount < 1 || clientCount > 100) {
			System.out
					.println("Value received:"
							+ args[2]
							+ ".Incorrect value of Clients. Please choose a value between 1 to 100.");
			System.exit(-1);
		}

		MainClient client = new MainClient();
		client.connect("192.168.8.128");

		// read the file and call transactions
		int count = client.readFile(args[0], Integer.parseInt(args[1]),
				clientCount);

		// After reading file, printing the output
		System.err.println("Total number of transactions processed : " + count);
		System.err
				.println("Total elapsed time for processing the transactions (in seconds): ");
		System.err
				.println("Transaction throughput (number of transactions processed per second): ");

		client.close();
		System.exit(0);
	}
}