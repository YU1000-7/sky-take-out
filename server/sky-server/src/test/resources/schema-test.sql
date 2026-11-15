CREATE TABLE IF NOT EXISTS employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(32) NOT NULL,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    sex VARCHAR(8) NOT NULL,
    id_number VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL,
    name VARCHAR(32) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE IF NOT EXISTS dish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    image VARCHAR(255),
    description VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE IF NOT EXISTS setmeal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(255),
    image VARCHAR(255),
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    create_user BIGINT,
    update_user BIGINT
);

CREATE TABLE IF NOT EXISTS setmeal_dish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setmeal_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    copies INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS shopping_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dish_id BIGINT,
    setmeal_id BIGINT,
    dish_flavor VARCHAR(64),
    number INT NOT NULL DEFAULT 1,
    amount DECIMAL(10, 2) NOT NULL,
    name VARCHAR(64) NOT NULL,
    image VARCHAR(255),
    create_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    address_book_id BIGINT NOT NULL,
    pay_status TINYINT NOT NULL DEFAULT 0,
    pay_method TINYINT NOT NULL DEFAULT 1,
    amount DECIMAL(10, 2) NOT NULL,
    order_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS order_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    image VARCHAR(255),
    order_id BIGINT NOT NULL,
    dish_id BIGINT,
    setmeal_id BIGINT,
    dish_flavor VARCHAR(64),
    number INT NOT NULL DEFAULT 1,
    amount DECIMAL(10, 2) NOT NULL
);
