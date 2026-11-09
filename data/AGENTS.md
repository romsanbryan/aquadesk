# Instrucciones del módulo `data`

Lee primero la guía de arquitectura de la raíz en
[`../AGENTES.md`](../AGENTES.md).

Este módulo contiene las entidades JPA en `src/main/java/romsanbryan/data/entities`
y sus interfaces Spring Data en `src/main/java/romsanbryan/data/repositories`.
No debe absorber lógica de presentación o de negocio propia de `app`.

Al cambiar el modelo:

- Comprueba relaciones, nulabilidad, longitudes y nombres de columnas usados
  por el esquema existente.
- Revisa cómo Hibernate y el esquema de [`../db/db.sql`](../db/db.sql) afectan a
  instalaciones ya desplegadas; no presupongas que una base nueva es el único
  destino.
- Mantén las consultas y operaciones relacionadas con datos de acuario
  correctamente acotadas por `aquariumId`.
- Actualiza los consumidores de la entidad/repositorio y añade pruebas de
  integración o persistencia en el módulo que ya tenga infraestructura para
  ello.
- No ejecutes migraciones ni operaciones destructivas contra bases de datos
  reales como parte de las pruebas.
