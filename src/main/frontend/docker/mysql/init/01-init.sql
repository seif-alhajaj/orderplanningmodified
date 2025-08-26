-- ===============================================
-- INITIALISATION BASE DE DONNÉES DOCKER
-- ===============================================
-- Fichier: docker/mysql/init/01-init.sql

-- Utiliser la base de données dev
USE dev;

-- ========== CRÉATION TABLE j_employee ==========
CREATE TABLE IF NOT EXISTS j_employee (
                                          id BINARY(16) NOT NULL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    work_hours_per_day INT DEFAULT 8,
    active BOOLEAN DEFAULT TRUE,
    creation_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    modification_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_employee_active (active),
    INDEX idx_employee_name (last_name, first_name),
    INDEX idx_employee_email (email)
    );

-- ========== CRÉATION TABLE order ==========
CREATE TABLE IF NOT EXISTS `order` (
                                       id BINARY(16) NOT NULL PRIMARY KEY,
    num_commande VARCHAR(50) NOT NULL UNIQUE,
    nombre_cartes INT NOT NULL DEFAULT 0,
    cartes_avec_nom INT DEFAULT 0,
    pourcentage_avec_nom DECIMAL(5,2) DEFAULT 0.00,
    prix_total DECIMAL(10,2) DEFAULT 0.00,
    priorite ENUM('EXCELSIOR', 'FAST+', 'FAST', 'CLASSIC') DEFAULT 'CLASSIC',
    statut INT DEFAULT 1,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    date_echeance DATE,
    duree_estimee_minutes INT DEFAULT 0,
    delai_code VARCHAR(20),

    INDEX idx_order_status (statut),
    INDEX idx_order_priority (priorite),
    INDEX idx_order_date (date_creation),
    INDEX idx_order_number (num_commande)
    );

-- ========== CRÉATION TABLE j_planning ==========
CREATE TABLE IF NOT EXISTS j_planning (
                                          id BINARY(16) NOT NULL PRIMARY KEY,
    order_id BINARY(16) NOT NULL,
    employee_id BINARY(16) NOT NULL,
    planning_date DATE NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    estimated_duration_minutes INT NOT NULL DEFAULT 0,
    priority ENUM('EXCELSIOR', 'FAST+', 'FAST', 'CLASSIC') DEFAULT 'CLASSIC',
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    completed BOOLEAN DEFAULT FALSE,
    card_count INT DEFAULT 1,
    notes TEXT,
    progress_percentage INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (order_id) REFERENCES `order`(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES j_employee(id) ON DELETE CASCADE,

    INDEX idx_planning_date (planning_date),
    INDEX idx_planning_employee (employee_id),
    INDEX idx_planning_order (order_id),
    INDEX idx_planning_status (status),
    INDEX idx_planning_start_time (start_time)
    );

-- ========== INSERTION DONNÉES DE TEST ==========

-- Employés de test
INSERT IGNORE INTO j_employee
(id, first_name, last_name, email, work_hours_per_day, active, creation_date, modification_date)
VALUES
(UNHEX(REPLACE(UUID(), '-', '')), 'Jean', 'Martin', 'jean.martin@pokemon.com', 8, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'Marie', 'Dupont', 'marie.dupont@pokemon.com', 8, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'Pierre', 'Bernard', 'pierre.bernard@pokemon.com', 7, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'Sophie', 'Leroy', 'sophie.leroy@pokemon.com', 8, 1, NOW(), NOW()),
(UNHEX(REPLACE(UUID(), '-', '')), 'Thomas', 'Moreau', 'thomas.moreau@pokemon.com', 6, 1, NOW(), NOW());

-- Commandes de test
INSERT IGNORE INTO `order`
(id, num_commande, nombre_cartes, cartes_avec_nom, pourcentage_avec_nom, prix_total, priorite, statut, date_creation, duree_estimee_minutes, delai_code)
VALUES
(UNHEX(REPLACE(UUID(), '-', '')), 'CMD001', 25, 20, 80.00, 150.00, 'EXCELSIOR', 1, NOW(), 75, 'EXCELSIOR'),
(UNHEX(REPLACE(UUID(), '-', '')), 'CMD002', 15, 12, 80.00, 90.00, 'FAST', 1, NOW(), 45, 'FAST'),
(UNHEX(REPLACE(UUID(), '-', '')), 'CMD003', 30, 25, 83.33, 200.00, 'FAST+', 1, NOW(), 90, 'FAST+'),
(UNHEX(REPLACE(UUID(), '-', '')), 'CMD004', 10, 8, 80.00, 60.00, 'CLASSIC', 1, NOW(), 30, 'CLASSIC'),
(UNHEX(REPLACE(UUID(), '-', '')), 'CMD005', 50, 40, 80.00, 350.00, 'EXCELSIOR', 1, NOW(), 150, 'EXCELSIOR');

-- Vérification
SELECT 'Employés créés:' as info, COUNT(*) as count FROM j_employee;
SELECT 'Commandes créées:' as info, COUNT(*) as count FROM `order`;

-- Afficher quelques données
SELECT
    CONCAT(first_name, ' ', last_name) as employee_name,
    email,
    work_hours_per_day as hours_per_day
FROM j_employee
ORDER BY last_name;

SELECT
    num_commande,
    nombre_cartes,
    priorite,
    duree_estimee_minutes
FROM `order`
ORDER BY date_creation;