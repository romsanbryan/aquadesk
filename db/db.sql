-- AquaDesk schema; select the target database before running this file.
SET NAMES utf8mb4;

CREATE TABLE `aquariums` (
  `id` int NOT NULL,
  `name` varchar(150) NOT NULL,
  `type` varchar(100) DEFAULT NULL,
  `volume` decimal(10,2) DEFAULT NULL,
  `glass_thickness` varchar(50) DEFAULT NULL,
  `size` varchar(100) DEFAULT NULL,
  `heater` varchar(100) DEFAULT NULL,
  `light` varchar(100) DEFAULT NULL,
  `substrate` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aquarium_filters` (
  `id` int NOT NULL,
  `aquarium_id` int NOT NULL,
  `name` varchar(50) DEFAULT NULL,
  `material_biological` varchar(255) DEFAULT NULL,
  `material_mechanical` varchar(255) DEFAULT NULL,
  `material_biological_mechanical` varchar(255) DEFAULT NULL,
  `sponge` tinyint(1) NOT NULL DEFAULT '0',
  `activated_carbon` tinyint(1) NOT NULL DEFAULT '0',
  `perlon` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aquarium_life_entries` (
  `id` int NOT NULL,
  `category` varchar(10) NOT NULL,
  `common_name` varchar(150) NOT NULL,
  `notes` text,
  `quantity` int NOT NULL,
  `recommended_parameters` text,
  `scientific_name` varchar(150) DEFAULT NULL,
  `technical_sheet_url` varchar(2048) DEFAULT NULL,
  `aquarium_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `aquarium_tasks` (
  `id` int NOT NULL,
  `action_date` date NOT NULL,
  `description` text,
  `title` varchar(150) NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `aquarium_id` int NOT NULL,
  `event_type` varchar(50) NOT NULL DEFAULT 'info'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `events` (
  `id` int NOT NULL,
  `aquarium_id` int NOT NULL,
  `title` varchar(100) NOT NULL,
  `start_date` date NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `clickable` tinyint(1) NOT NULL DEFAULT '1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_details` (
  `id` int NOT NULL,
  `event_id` int NOT NULL,
  `observation` mediumtext,
  `detail_type` varchar(100) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_maintenances` (
  `id` int NOT NULL,
  `event_id` int NOT NULL,
  `water` int NOT NULL,
  `prime_dose` decimal(38,2) DEFAULT NULL,
  `observations` varchar(500) NOT NULL,
  `glass_cleaned` tinyint(1) NOT NULL DEFAULT '0',
  `pruning` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_parameters` (
  `id` bigint NOT NULL,
  `event_id` int NOT NULL,
  `nh3` decimal(5,2) DEFAULT NULL,
  `no2` decimal(5,2) DEFAULT NULL,
  `no3` decimal(5,2) DEFAULT NULL,
  `ph` decimal(4,2) DEFAULT NULL,
  `kh` decimal(5,2) DEFAULT NULL,
  `gh` decimal(5,2) DEFAULT NULL,
  `temperature` decimal(5,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_photos` (
  `id` int NOT NULL,
  `event_id` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `photo_data` longblob NOT NULL,
  `mime_type` varchar(50) NOT NULL DEFAULT 'image/jpeg'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_treatments` (
  `id` int NOT NULL,
  `event_id` int NOT NULL,
  `dose` decimal(38,2) NOT NULL,
  `product_name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `maintenance_filters` (
  `id` int NOT NULL,
  `maintenance_id` int NOT NULL,
  `filter_name` varchar(100) NOT NULL,
  `sponge` tinyint(1) DEFAULT '0',
  `perlon` tinyint(1) DEFAULT '0',
  `mechanical` tinyint(1) DEFAULT '0',
  `biological` tinyint(1) DEFAULT '0',
  `biological_mechanical` tinyint(1) DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notifications` (
  `id` bigint NOT NULL,
  `aquarium_id` int NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `parameter` varchar(100) NOT NULL,
  `value` int NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `weekdays` int NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `parameter_ranges` (
  `id` int NOT NULL,
  `aquarium_id` int NOT NULL,
  `parameter` varchar(100) NOT NULL COMMENT 'NH3, NO2, NO3, pH, KH, GH, etc',
  `level_1` decimal(10,2) DEFAULT NULL,
  `level_2` decimal(10,2) DEFAULT NULL,
  `level_3` decimal(10,2) DEFAULT NULL,
  `level_4` decimal(10,2) DEFAULT NULL,
  `level_5` decimal(10,2) DEFAULT NULL,
  `unit` varchar(50) DEFAULT NULL COMMENT 'ppm, dKH, etc',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `aquariums`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `aquarium_filters`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_aquarium_filters_aquarium_id` (`aquarium_id`);

ALTER TABLE `aquarium_life_entries`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_aquarium_life_entries_aquarium_category` (`aquarium_id`,`category`);

ALTER TABLE `aquarium_tasks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_aquarium_tasks_aquarium_date` (`aquarium_id`,`action_date`);

ALTER TABLE `events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_events_aquarium_start_date_type` (`aquarium_id`,`start_date`,`event_type`);

ALTER TABLE `event_details`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_event_details_event_id` (`event_id`);

ALTER TABLE `event_maintenances`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_event_maintenances_event_id` (`event_id`);

ALTER TABLE `event_parameters`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_event_parameters_event_id` (`event_id`);

ALTER TABLE `event_photos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_event_photos_event_id` (`event_id`);

ALTER TABLE `event_treatments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_event_treatments_event_id` (`event_id`);

ALTER TABLE `maintenance_filters`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_maintenance_filters_maintenance_id` (`maintenance_id`);

ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_notifications_aquarium_active` (`aquarium_id`,`active`);

ALTER TABLE `parameter_ranges`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_parameter_ranges_aquarium_parameter` (`aquarium_id`,`parameter`);

ALTER TABLE `aquariums`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `aquarium_filters`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `aquarium_life_entries`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `aquarium_tasks`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `events`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `event_details`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `event_maintenances`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `event_parameters`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `event_photos`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `event_treatments`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `maintenance_filters`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `notifications`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `parameter_ranges`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

ALTER TABLE `aquarium_filters`
  ADD CONSTRAINT `fk_aquarium_filters_aquarium` FOREIGN KEY (`aquarium_id`) REFERENCES `aquariums` (`id`) ON DELETE CASCADE;

ALTER TABLE `aquarium_life_entries`
  ADD CONSTRAINT `fk_aquarium_life_entries_aquarium` FOREIGN KEY (`aquarium_id`) REFERENCES `aquariums` (`id`) ON DELETE CASCADE;

ALTER TABLE `aquarium_tasks`
  ADD CONSTRAINT `fk_aquarium_tasks_aquarium` FOREIGN KEY (`aquarium_id`) REFERENCES `aquariums` (`id`) ON DELETE CASCADE;

ALTER TABLE `events`
  ADD CONSTRAINT `fk_events_aquarium` FOREIGN KEY (`aquarium_id`) REFERENCES `aquariums` (`id`) ON DELETE CASCADE;

ALTER TABLE `event_details`
  ADD CONSTRAINT `fk_event_details_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

ALTER TABLE `event_maintenances`
  ADD CONSTRAINT `fk_event_maintenances_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

ALTER TABLE `event_parameters`
  ADD CONSTRAINT `fk_event_parameters_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

ALTER TABLE `event_photos`
  ADD CONSTRAINT `fk_event_photos_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

ALTER TABLE `event_treatments`
  ADD CONSTRAINT `fk_event_treatments_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;

ALTER TABLE `maintenance_filters`
  ADD CONSTRAINT `fk_maintenance_filters_event_maintenances` FOREIGN KEY (`maintenance_id`) REFERENCES `event_maintenances` (`id`) ON DELETE CASCADE;

ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notifications_aquarium` FOREIGN KEY (`aquarium_id`) REFERENCES `aquariums` (`id`) ON DELETE CASCADE;

ALTER TABLE `parameter_ranges`
  ADD CONSTRAINT `fk_parameter_ranges_aquarium` FOREIGN KEY (`aquarium_id`) REFERENCES `aquariums` (`id`) ON DELETE CASCADE;

ALTER TABLE `aquariums`
    ADD CONSTRAINT `ck_aquariums_volume_nonnegative`
        CHECK (`volume` IS NULL OR `volume` >= 0);

ALTER TABLE `aquarium_life_entries`
    ADD CONSTRAINT `ck_aquarium_life_entries_category`
        CHECK (`category` IN ('fish', 'plant')),
    ADD CONSTRAINT `ck_aquarium_life_entries_quantity_nonnegative`
        CHECK (`quantity` >= 0);

ALTER TABLE `notifications`
    ADD CONSTRAINT `ck_notifications_active`
        CHECK (`active` IN (0, 1)),
    ADD CONSTRAINT `ck_notifications_value_positive`
        CHECK (`value` > 0);

ALTER TABLE `aquarium_filters`
    ADD CONSTRAINT `ck_aquarium_filters_flags`
        CHECK (`sponge` IN (0, 1) AND `activated_carbon` IN (0, 1) AND `perlon` IN (0, 1));

ALTER TABLE `aquarium_tasks`
    ADD CONSTRAINT `ck_aquarium_tasks_event_type`
        CHECK (
            CAST(`event_type` AS BINARY) IN (
                CAST('info' AS BINARY),
                CAST('live' AS BINARY),
                CAST('co2' AS BINARY),
                CAST('manteiment' AS BINARY),
                CAST('parameters' AS BINARY),
                CAST('photo' AS BINARY)
            )
        );

ALTER TABLE `events`
    ADD CONSTRAINT `ck_events_clickable`
        CHECK (`clickable` IN (0, 1));

ALTER TABLE `event_maintenances`
    ADD CONSTRAINT `ck_event_maintenances_glass_cleaned`
        CHECK (`glass_cleaned` IN (0, 1)),
    ADD CONSTRAINT `ck_event_maintenances_water_nonnegative`
        CHECK (`water` >= 0),
    ADD CONSTRAINT `ck_event_maintenances_prime_dose_nonnegative`
        CHECK (`prime_dose` IS NULL OR `prime_dose` >= 0);

ALTER TABLE `event_treatments`
    ADD CONSTRAINT `ck_event_treatments_dose_nonnegative`
        CHECK (`dose` >= 0);

ALTER TABLE `maintenance_filters`
    ADD CONSTRAINT `ck_maintenance_filters_flags`
        CHECK (
            (`sponge` IS NULL OR `sponge` IN (0, 1))
            AND (`perlon` IS NULL OR `perlon` IN (0, 1))
            AND (`mechanical` IS NULL OR `mechanical` IN (0, 1))
            AND (`biological` IS NULL OR `biological` IN (0, 1))
            AND (`biological_mechanical` IS NULL OR `biological_mechanical` IN (0, 1))
        );
