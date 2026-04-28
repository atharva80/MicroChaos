package com.microchaos.backend.core;

public class DbTest {
    public static void main(String[] args) {
        System.out.println("=== Testing Database Connection ===");
        
        DatabaseConnection db = new DatabaseConnection();
        db.connect();
        
        if (db.isConnected()) {
            System.out.println("🎉 Connection test PASSED!");
        } else {
            System.out.println("⚠️ Connection test FAILED - running in fallback mode");
        }
        
        db.close();
    }
}