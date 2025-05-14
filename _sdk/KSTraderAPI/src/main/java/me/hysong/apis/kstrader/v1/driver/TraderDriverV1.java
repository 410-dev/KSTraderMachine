package me.hysong.apis.kstrader.v1.driver;

import me.hysong.apis.kstrader.v1.objects.Account;
import me.hysong.apis.kstrader.v1.objects.Chart;
import me.hysong.apis.kstrader.v1.objects.Order;

import java.util.ArrayList;
import java.util.HashMap;

public interface TraderDriverV1 {

    /**
     * Get chart data
     * @param account Account that contains credentials
     * @param params Parameters for the chart data.
     *               Keys may require:
     *               - symbol: The symbol to get chart data for.
     *               - interval: The interval for thechart data.
     *               - startTime: The start time for the chart data.
     *               - endTime: The end time for the chart data.
     *               - limit: The limit for the chart data.
     * @return A chart object representing the chart data.
     * @throws Exception if an error occurs while getting chart data
     */
    Chart getChart(Account account, HashMap<String, Object> params) throws Exception;

    /**
     * Get orders
     * @param account Account that contains credentials
     * @param params Parameters for the orders.
     *               Keys may require:
     *               - symbol: The symbol to get orders for.
     *               - orderId: The order ID to get orders for.
     *               - limit: The limit for the orders.
     * @return An Array of Order objects representing the orders.
     * @throws Exception if an error occurs while getting orders
     */
    ArrayList<Order> getOrders(Account account, HashMap<String, Object> params) throws Exception;

    /**
     * Get account information
     * @param account Account that contains credentials
     * @return An Account object representing the account information.
     * @throws Exception if an error occurs while getting account information
     */
    Account getAccount(Account account) throws Exception;

    /**
     * Make a trade
     * @param account Account that contains credentials
     * @param order Orders to be placed.
     * @param params Parameters for the trade.
     * @return An Array of Order objects representing the orders.
     * @throws Exception if an error occurs while making a trade
     */
    Order placeOrder(Account account, Order order, HashMap<String, Object> params) throws Exception;

    /**
     * Place orders as batch
     * @param account Account that contains credentials
     * @param orders Orders to be placed.
     * @param params Parameters for the trade.
     * @return An Array of Order objects representing the orders.
     * @throws Exception if an error occurs while placing orders
     */
    ArrayList<Order> placeOrders(Account account, ArrayList<Order> orders, HashMap<String, Object> params) throws Exception;

    /**
     * Cancel an order
     * @param account Account that contains credentials
     * @param orderId The ID of the order to be canceled.
     * @param params Parameters for the cancelation.
     * @return An Order object representing the canceled order.
     * @throws Exception if an error occurs while canceling an order
     */
    Order cancelOrder(Account account, String orderId, HashMap<String, Object> params) throws Exception;

    /**
     * Cancel order as batch
     * @param account Account that contains credentials
     * @param orderIds The IDs of the orders to be canceled.
     * @param params Parameters for the cancelation.
     * @return An Array of Order objects representing the canceled orders.
     * @throws Exception if an error occurs while canceling orders
     */
    ArrayList<Order> cancelOrders(Account account, ArrayList<String> orderIds, HashMap<String, Object> params) throws Exception;
}
