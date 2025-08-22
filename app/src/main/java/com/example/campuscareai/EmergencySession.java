package com.example.campuscareai;
public class EmergencySession {
    public String userId;
    public String userName;
    public double latitude;
    public double longitude;
    public String status;

    public EmergencySession() {} // Firebase needs empty constructor

    public EmergencySession(String userId, String userName, double latitude, double longitude, String status) {
        this.userId = userId;
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }
}
