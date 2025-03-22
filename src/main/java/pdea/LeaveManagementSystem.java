package pdea;
import java.sql.*;
import java.util.Scanner;

public class LeaveManagementSystem {
    private static final String URL = "jdbc:postgresql://localhost:5432/lms_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "123";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // Main menu
        	System.out.println("\n*****WELCOME*****");
            System.out.println("\nLeave Management System");
            System.out.println("1. Register Employee");
            System.out.println("2. Employee Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            if (choice == 1) {
                registerEmployee(scanner);
            } else if (choice == 2) {
                System.out.print("Enter Employee ID: ");
                int empId = scanner.nextInt();
                scanner.nextLine();
                employeeMenu(scanner, empId);
            } else if (choice == 3) {
                System.out.println("Thanks for using Leave Management System\n   Exiting...");
                break;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }//while loop
        scanner.close();
    }//main
    private static void registerEmployee(Scanner scanner) {
        // Employee registration
        System.out.print("Enter Employee Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Employee Department: ");
        String department = scanner.nextLine();
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            String sql = "INSERT INTO employees (name, department, paid_leave, casual_leave, sick_leave, unpaid_leave) VALUES ('" + name + "', '" + department + "', 12, 6, 6, 0)";
            stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int empId = rs.getInt(1);
                System.out.println("Employee registered successfully. Employee ID: " + empId);
            }
        } catch (SQLException e) {
            System.out.println("Error occurred during registration.");
        }
    }//registerEmployee


    private static void employeeMenu(Scanner scanner, int empId) {
        while (true) {
            // Employee menu options
            System.out.println("\nEmployee Menu");
            System.out.println("1. Apply for Leave");
            System.out.println("2. View Leave Balance");
            System.out.println("3. View Leave Summary");
            System.out.println("4. Logout");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            if (choice == 1) {
                applyLeave(scanner, empId);
            } else if (choice == 2) {
                viewLeaveBalance(empId);
            } else if (choice == 3) {
                viewLeaveSummary(empId);
            } else if (choice == 4) {
                System.out.println("Logging out...");
                break;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
    }//employeeMenu
    
    private static void applyLeave(Scanner scanner, int empId) {
        System.out.print("Enter Leave Type (paid, casual, sick): ");
        String leaveType = scanner.nextLine().toLowerCase();
        System.out.print("Enter Days: ");
        int days = scanner.nextInt();
        scanner.nextLine();

        if (!leaveType.equals("paid") && !leaveType.equals("casual") && !leaveType.equals("sick")) {
            System.out.println("Invalid leave type.");
            return;
        }

        String query = "SELECT " + leaveType + "_leave, unpaid_leave FROM employees WHERE id = ?";
        String updateLeave = "UPDATE employees SET " + leaveType + "_leave = CASE WHEN " + leaveType + "_leave >= ? THEN " + leaveType + "_leave - ? ELSE 0 END, unpaid_leave = unpaid_leave + ? WHERE id = ?";
        String insertLeave = "INSERT INTO leaves (emp_id, leave_type, days, status) VALUES (?, ?, ?, 'Approved')";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query);
             PreparedStatement updateStmt = conn.prepareStatement(updateLeave);
             PreparedStatement insertStmt = conn.prepareStatement(insertLeave)) {

            pstmt.setInt(1, empId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int availableLeave = rs.getInt(1);
                @SuppressWarnings("unused")
				int unpaidLeave = rs.getInt(2);
                int approvedLeave = Math.min(availableLeave, days);
                int unpaidDays = days - approvedLeave;

                updateStmt.setInt(1, days);
                updateStmt.setInt(2, days);
                updateStmt.setInt(3, unpaidDays);
                updateStmt.setInt(4, empId);
                updateStmt.executeUpdate();

                insertStmt.setInt(1, empId);
                insertStmt.setString(2, capitalize(leaveType));
                insertStmt.setInt(3, approvedLeave);
                insertStmt.executeUpdate();

                if (unpaidDays > 0) {
                    insertStmt.setInt(1, empId);
                    insertStmt.setString(2, "Unpaid");
                    insertStmt.setInt(3, unpaidDays);
                    insertStmt.executeUpdate();
                }

                System.out.println("Leave applied successfully. " +
                        approvedLeave + " days of " + leaveType + " leave approved. " +
                        (unpaidDays > 0 ? unpaidDays + " days marked as unpaid." : ""));
            }
        } catch (SQLException e) {
            System.out.println("Error occurred while applying for leave: " + e.getMessage());
        }
    }//applyLeave

    private static void viewLeaveBalance(int empId) {
        // Viewing leave balance
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT paid_leave, casual_leave, sick_leave, unpaid_leave FROM employees WHERE id = ?")) {
            stmt.setInt(1, empId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\nLeave Balance:");
                System.out.println("Paid Leave: " + rs.getInt("paid_leave"));
                System.out.println("Casual Leave: " + rs.getInt("casual_leave"));
                System.out.println("Sick Leave: " + rs.getInt("sick_leave"));
                System.out.println("Unpaid Leave: " + rs.getInt("unpaid_leave"));
            }
        } catch (SQLException e) {
            System.out.println("Error occurred while fetching leave balance.");
        }
    }//viewLeaveBalance
    
    private static void viewLeaveSummary(int empId) {
        // Query to get leave summary
        String query = "SELECT leave_type, SUM(days) as total_days FROM leaves WHERE emp_id = ? GROUP BY leave_type";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, empId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("\nLeave Summary:");
            while (rs.next()) {
                System.out.println(rs.getString("leave_type") + " Leave: " + rs.getInt("total_days") + " days");
            }
        } catch (SQLException e) {
            System.out.println("Error occurred while fetching leave summary.");
        }
    }//viewLeaveSummary
    
    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }//capitalize
    
}//LeaveManagementSystem
