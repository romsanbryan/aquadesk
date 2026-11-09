# AquaDesk

AquaDesk helps you organize multiple aquariums in one web application. You can view each aquarium's calendar and history, record maintenance, and schedule reminders for future tasks.

## What you can do

- Manage multiple aquariums while keeping their records separate.
- Record information about fish and plants, maintenance, water parameters, CO₂ treatments, and photos.
- View calendar events and review their history.
- Schedule reminders by date and event type.
- Review and edit an event's details before saving it to the history when completing a reminder.
- View upcoming events and configure notifications.

## Getting started

Open AquaDesk in your browser and select the aquarium you want to manage. From its management page, you can view its calendar, create events, and manage reminders.

The [User Guide](USER_GUIDE.md) provides step-by-step instructions, explains the information requested when completing each reminder type, and describes where to edit form fields if you customize the application.

## Reminders

When creating a reminder, choose the event type that will be recorded when it is completed. Select **Complete** to review and edit the event details before confirming. For a photo reminder, also select the image you want to save.

Once completed successfully, the event is added to the history and the reminder is removed from the pending task list. See [Creating and completing reminders](USER_GUIDE.md#create-and-complete-reminders) for more information.

## Database schema

[`db/db.sql`](db/db.sql) contains AquaDesk's initial schema, without sample data. To install it, create and select an empty MySQL database, then execute the file in that database; the script does not create or select a database. Afterward, configure the application to connect to it.

The schema defines tables, keys, indexes, `CHECK` constraints, and relationships using `InnoDB` and `utf8mb4`. It is designed for MySQL 8.4. Do not run it against an existing database: it is an initial schema, not a migration.
