package com.paytracker;

public class Transaction {
    public long time;
    public boolean received; // true = money in, false = money out
    public String player;
    public double amount;

    public Transaction(long time, boolean received, String player, double amount) {
        this.time = time;
        this.received = received;
        this.player = player;
        this.amount = amount;
    }
}
