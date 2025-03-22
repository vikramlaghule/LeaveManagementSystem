CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    paid_leave INT DEFAULT 12,
    casual_leave INT DEFAULT 6,
    sick_leave INT DEFAULT 6,
    unpaid_leave INT DEFAULT 0
);
CREATE TABLE leaves (
    id SERIAL PRIMARY KEY,
    emp_id INT REFERENCES employees(id) ON DELETE CASCADE,
    leave_type VARCHAR(20) CHECK (leave_type IN ('Paid', 'Casual', 'Sick', 'Unpaid')),
    days INT NOT NULL,
    status VARCHAR(20) DEFAULT 'Approved',
    applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

