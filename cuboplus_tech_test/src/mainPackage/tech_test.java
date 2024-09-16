/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mainPackage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.json.JSONArray;
import org.json.JSONObject;

public class tech_test {
    private static final String ADDRESS = "32ixEdVJWo3kmvJGMTZq5jAQVZZeuwnqzo";
    private static final String API_URL = "https://mempool.space/api";

    public static void main(String[] args) {
        try {
            JSONObject addressInfo = getJSON(API_URL + "/address/" + ADDRESS);            
            // Calculate the on-chain balance
            long onChainBalance = addressInfo.getJSONObject("chain_stats").getLong("funded_txo_sum") -                                   
                                  addressInfo.getJSONObject("chain_stats").getLong("spent_txo_sum");
            System.out.println("1. On-chain Balance: " + onChainBalance + " satoshis");
            // Calculate the mempool Balance
            long mempoolBalance = addressInfo.getJSONObject("mempool_stats").getLong("funded_txo_sum") -
                                  addressInfo.getJSONObject("mempool_stats").getLong("spent_txo_sum");
            System.out.println("2. Mempool Balance: " + mempoolBalance + " satoshis");
            JSONArray transactions = getJSONArray(API_URL + "/address/" + ADDRESS + "/txs");
            // Calculate the balance variation in a period of 30 days
            long balanceChange30Days = calculateOnChainBalance(transactions, 30);
            System.out.println("3. Balance variation in last 30 days: " + balanceChange30Days + " satoshis");
            // Calculate the balance variation in a period of 7 days
            long balanceChange7Days = calculateOnChainBalance(transactions, 7);
            System.out.println("4. Balance variation in last 7 days: " + balanceChange7Days + " satoshis");
        } catch (Exception e) {
        }
    }

    private static JSONObject getJSON(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return new JSONObject(response.toString());
    }

    private static JSONArray getJSONArray(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        StringBuilder response;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return new JSONArray(response.toString());
    }

    private static long calculateOnChainBalance(JSONArray transactions, int days) {
        long balanceChange = 0;
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(days);

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject transact = transactions.getJSONObject(i);
            LocalDateTime transactDate = getTransactDate(transact);
            
            if (transactDate == null || transactDate.isAfter(cutOffDate)) {
                JSONArray vins = transact.getJSONArray("vin");
                for (int j = 0; j < vins.length(); j++) {
                    JSONObject vin = vins.getJSONObject(j);
                    if (vin.has("prevout")) {
                        JSONObject prevout = vin.getJSONObject("prevout");
                        if (prevout.has("scriptpubkey_address") && ADDRESS.equals(prevout.getString("scriptpubkey_address"))) {
                            balanceChange -= prevout.getLong("value");
                        }
                    }
                }
                JSONArray vouts = transact.getJSONArray("vout");
                for (int j = 0; j < vouts.length(); j++) {
                    JSONObject vout = vouts.getJSONObject(j);
                    if (vout.has("scriptpubkey_address") && ADDRESS.equals(vout.getString("scriptpubkey_address"))) {
                        balanceChange += vout.getLong("value");
                    }
                }
            }
        }
        return balanceChange;
    }

    private static LocalDateTime getTransactDate(JSONObject transact) {
        JSONObject status = transact.getJSONObject("status");
        if (status.has("block_time")) {
            return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(status.getLong("block_time")),
                ZoneId.systemDefault()
            );
        } else if (status.has("confirmed")) {
            return LocalDateTime.now();
        } else {
            return LocalDateTime.now();
        }
    }
}