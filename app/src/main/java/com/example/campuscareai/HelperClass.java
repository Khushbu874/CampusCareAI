package com.example.campuscareai;
public class HelperClass {
    String name, email, enrollment, phone, department, course, branch, year;

    public HelperClass() {
    }

    public HelperClass(String name, String email, String enrollment, String phone,
                       String department, String course, String branch, String year) {
        this.name = name;
        this.email = email;
        this.enrollment = enrollment;
        this.phone = phone;
        this.department = department;
        this.course = course;
        this.branch = branch;
        this.year = year;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEnrollment() { return enrollment; }
    public void setEnrollment(String enrollment) { this.enrollment = enrollment; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}

