# AquaDesk User Guide

This guide describes the usual use of AquaDesk and how reminders work. For an introduction to the product, see the [README](README.md).

## Getting started

1. Open AquaDesk in your browser.
2. From the aquarium list, select the aquarium you want to view or manage.
3. Use its calendar, history, and management options to work with its data.

Records and reminders belong to the selected aquarium. Make sure you have selected the correct aquarium before creating or completing a record.

## Recording events

From the management page, you can record daily information, fish or plant details, maintenance, water parameters, CO₂ treatments, and photos. Fill in the event fields and save your changes to add the event to the history and calendar.

## Create and complete reminders

1. On the management page, select **Create reminder**.
2. Enter the task name, set the date, and choose the event type that will be created when you complete it.
3. Add a description if you want to keep instructions or context for later, then save the reminder.
4. When you are ready to carry it out, open the reminder from the list or calendar and select **Complete**.
5. Review the form for the selected type, fill in or correct the details, and confirm. For the **Photo** type, also select the image file.

The completion form defaults to the current date and may suggest the reminder description as an observation. You can edit these values before confirming.

The available types are:

| Type | Event details |
|---|---|
| Information | Information subtype, status, and observation. |
| Fish or plants | Choose whether the record is for fish or plants; enter the status and observation. |
| Maintenance | Water change, Prime and Profito doses, glass cleaning, pruning, observations, and filter tasks. |
| Parameters | One or more water values, such as NH3, NO2, NO3, pH, KH, GH, or temperature. |
| CO₂ treatment | Product and value or dose. |
| Photo | An image to add to the history. |

If required information is missing—for example, a product and value for CO₂, at least one parameter, or a photo—AquaDesk will not complete the reminder. Correct the form and try again; the reminder remains available until it is completed successfully.

When the reminder is completed successfully, AquaDesk creates the corresponding event in the history and removes the reminder from the pending tasks. If you are not ready to record it yet, you can cancel the form and return to the reminder details.

## Edit or delete a reminder

Open the reminder and select **Edit** to change its name, date, description, or event type. Select **Delete** if you no longer need the task. Deleting a reminder does not create an event in the history.

## Customize completion form fields

This section is for people modifying the AquaDesk code. The form is in:

`app/src/main/resources/templates/gestion.html`

Look for `id="task-completion-form"`. Field groups are separated by type using `data-event-type` attributes, for example `data-event-type="manteiment"` or `data-event-type="parameters"`. You can remove a specific field or an entire group from this template.

If you remove a field, also update `app/src/main/resources/static/gestion.js`: form submission reads elements by their `id` and builds the completion JSON. Otherwise, JavaScript may fail when it cannot find the element. If you remove an entire type, also remove it from the type options in the reminder creation and editing forms, and review how that type is handled in `app/src/main/java/romsanbryan/aquadesk/services/AquariumTaskService.java`. The request contract is in `app/src/main/java/romsanbryan/aquadesk/dto/request/TaskCompletionRequest.java`.

Keep the form, JavaScript, and server-side validation in sync. After customizing them, test creating and completing reminders for each type that remains available.
