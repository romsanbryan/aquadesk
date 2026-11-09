package romsanbryan.aquadesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.entities.Event;
import romsanbryan.data.repositories.AquariumLifeRepository;
import romsanbryan.data.repositories.AquariumRepository;
import romsanbryan.data.repositories.AquariumTaskRepository;
import romsanbryan.data.repositories.EventRepository;
import romsanbryan.data.repositories.PhotoRepository;
import romsanbryan.data.repositories.MaintenanceRepository;
import romsanbryan.data.repositories.ParametersRangeRepository;
import romsanbryan.data.repositories.ParametersRepository;
import romsanbryan.data.repositories.AquariumFilterRepository;
import romsanbryan.data.repositories.NotificationsParameters;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicationEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private AquariumLifeRepository lifeRepository;

    @Autowired
    private AquariumTaskRepository taskRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ParametersRangeRepository rangeRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private ParametersRepository parametersRepository;

    @Autowired
    private AquariumFilterRepository filterRepository;

    @Autowired
    private NotificationsParameters notificationRepository;

    @Test
    void servesMainPagesAndStyledNotFoundPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/acuario"));

        mockMvc.perform(get("/acuario"))
                .andExpect(status().isOk())
            .andExpect(content().string(containsString("AquaDesk")))
            .andExpect(content().string(containsString("<html lang=\"es\">")))
            .andExpect(content().string(containsString(">Acuarios</h1>")))
            .andExpect(content().string(containsString("id=\"aquarium-create-form\" method=\"post\"")))
            .andExpect(content().string(containsString("id=\"aquarium-list\"")))
            .andExpect(content().string(containsString("src=\"/aquarium-list.js\"")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("${aquariums}"))));
        mockMvc.perform(get("/aquarium-list.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("request('/api/aquariums', 'GET')")));

        mockMvc.perform(post("/acuario"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/acuario").header("Accept-Language", "en"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(">Acuarios</h1>")));

        mockMvc.perform(get("/gestion"))
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/acuario"));
        mockMvc.perform(post("/gestion"))
                .andExpect(status().isMethodNotAllowed());

        Aquarium aquarium = createAquarium("Acuario de idioma");
        mockMvc.perform(get("/gestion").param("aquariumId", aquarium.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<html lang=\"es\">")))
            .andExpect(content().string(containsString("Próximos eventos")))
            .andExpect(content().string(containsString("id=\"life-date\"")))
            .andExpect(content().string(containsString("id=\"ctx-add-life\"")))
            .andExpect(content().string(containsString("src=\"/gestion.js\"")));

        mockMvc.perform(get("/gestion.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("./js/notifications.js")));
        mockMvc.perform(get("/js/notifications.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("initializeNotificationManagement")));
        mockMvc.perform(get("/js/parameter-ranges.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("initializeParameterRangeManagement")));
        mockMvc.perform(get("/js/last-photos.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("createLastPhotosLoader")));
        mockMvc.perform(get("/js/dashboard-data.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("initializeDashboardData")));
        mockMvc.perform(get("/js/utils.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("export function escapeHtml")));
        mockMvc.perform(get("/js/history-export.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("initializeHistoryExport")));
        mockMvc.perform(get("/js/life.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("initializeLifeManagement")));

        mockMvc.perform(get("/api/updates"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        mockMvc.perform(get("/route-that-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(get("/error")
                        .requestAttr("jakarta.servlet.error.status_code", 404)
                        .requestAttr("jakarta.servlet.error.request_uri", "/route-that-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("404")));
    }

    @Test
    void aquariumListIsLoadedFromApiInsteadOfThymeleafModel() throws Exception {
        Aquarium aquarium = createAquarium("API aquarium");
        mockMvc.perform(post("/api/aquariums/{aquariumId}/filters", aquarium.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Canister","materialBiological":"Rings",
                                 "materialMechanical":"Sponge","materialBioMechanical":"Matrix",
                                 "sponge":true,"carbonActive":false,"perlon":true}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/acuario"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("API aquarium"))));

        mockMvc.perform(get("/api/aquariums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("API aquarium"))
                .andExpect(jsonPath("$[0].filters[0].name").value("Canister"))
                .andExpect(jsonPath("$[0].filters[0].sponge").value(true));
    }

    @Test
    void createsAquariumAndFiltersAndDeletesTheAquariumData() throws Exception {
        mockMvc.perform(post("/api/aquariums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","filters":[]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/aquariums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Integration aquarium","type":"Freshwater","volume":95.5,
                                 "filters":[{"name":"Canister","materialBiological":"Rings",
                                 "materialMechanical":"Sponge","materialBioMechanical":"Matrix",
                                 "sponge":true,"carbonActive":false,"perlon":true}]}
                                """))
                .andExpect(status().isCreated());

        Aquarium aquarium = aquariumRepository.findAll().stream()
                .filter(item -> "Integration aquarium".equals(item.getName()))
                .findFirst().orElseThrow();
        int aquariumId = aquarium.getId();

        mockMvc.perform(put("/api/aquariums/{aquariumId}", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated aquarium","type":"Freshwater","volume":100.0,
                                 "glassThickness":"6 mm","size":"80x40x40","heater":"100 W",
                                 "light":"LED","substrate":"Sand"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/gestion").param("aquariumId", Integer.toString(aquariumId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aquariumId")))
                .andExpect(content().string(containsString("history-export-form")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("history-export-notifications"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("history-export-tasks"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("history-export-photos"))));

        mockMvc.perform(post("/api/aquariums/{aquariumId}/filters", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sponge filter","materialBiological":"Bio media",
                                 "materialMechanical":"Foam","materialBioMechanical":"Composite",
                                 "sponge":true,"carbonActive":false,"perlon":false}
                                """))
                .andExpect(status().isCreated());
        var filter = filterRepository.findAll().stream()
                                .filter(item -> item.getAquarium().getId().equals(aquariumId))
                                .findFirst().orElseThrow();
        mockMvc.perform(put("/api/aquariums/{aquariumId}/filters/{filterId}", aquariumId, filter.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"name":"Updated filter","materialBiological":"Bio",
                                                 "materialMechanical":"Mechanical","materialBioMechanical":"Mixed",
                                                 "sponge":false,"carbonActive":true,"perlon":true}
                                                """))
                                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/filters/{filterId}", aquariumId, filter.getId()))
                                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/life", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"plant","commonName":"Java fern","quantity":2}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/aquariums/{aquariumId}", aquariumId))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(aquariumRepository.existsById(aquariumId));
        org.junit.jupiter.api.Assertions.assertTrue(lifeRepository
                .findByAquariumIdOrderByCategoryAscCommonNameAsc(aquariumId).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(taskRepository.findByAquariumIdOrderByActionDateAscIdAsc(aquariumId).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(eventRepository.findByAquariumIdAndStartDateBetween(
                aquariumId, java.time.LocalDate.of(1900, 1, 1), java.time.LocalDate.MAX).isEmpty());
    }

    @Test
    void registersAquariumLifeAndTracksScopedQuantityChangesInCalendar() throws Exception {
        int aquariumId = createAquarium("Life aquarium").getId();
        int otherAquariumId = createAquarium("Other life aquarium").getId();

        mockMvc.perform(post("/api/aquariums/{aquariumId}/life", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"unknown","commonName":"Neon tetra","quantity":2}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/aquariums/{aquariumId}/life", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"fish","commonName":"Neon tetra","quantity":3,
                                 "scientificName":"Paracheirodon innesi",
                                 "recommendedParameters":"24-26 C, pH 6.0-7.0",
                                 "technicalSheetUrl":"https://example.test/neon.pdf",
                                 "notes":"Keep in a school","date":"2026-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("fish"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.scientificName").value("Paracheirodon innesi"));

        var fish = lifeRepository.findByAquariumIdOrderByCategoryAscCommonNameAsc(aquariumId).get(0);
        mockMvc.perform(get("/api/aquariums/{aquariumId}/life", otherAquariumId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(patch("/api/aquariums/{aquariumId}/life/{lifeId}/quantity",
                        otherAquariumId, fish.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"change":1}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/aquariums/{aquariumId}/life/{lifeId}/quantity", aquariumId, fish.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"change":2,"date":"2026-01-02"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
        mockMvc.perform(patch("/api/aquariums/{aquariumId}/life/{lifeId}/quantity", aquariumId, fish.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"change":-4,"date":"2026-01-03"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1));
        mockMvc.perform(patch("/api/aquariums/{aquariumId}/life/{lifeId}/quantity", aquariumId, fish.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"change":-2}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/aquariums/{aquariumId}/life", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"plant","commonName":"Java fern","quantity":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("plant"));

        org.junit.jupiter.api.Assertions.assertEquals(3,
                eventRepository.findByAquariumIdOrderByStartDateAsc(aquariumId).stream()
                        .filter(event -> "life-fish".equals(event.getType())).count());
        org.junit.jupiter.api.Assertions.assertTrue(eventRepository.findByAquariumIdOrderByStartDateAsc(aquariumId)
                .stream().anyMatch(event -> "life-fish".equals(event.getType())
                        && LocalDate.of(2026, 1, 1).equals(event.getStartDate())));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                eventRepository.findByAquariumIdOrderByStartDateAsc(aquariumId).stream()
                        .filter(event -> "life-plant".equals(event.getType())).count());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                eventRepository.findByAquariumIdOrderByStartDateAsc(aquariumId).stream()
                        .filter(event -> event.getTitle().contains("4") && event.getTitle().contains("Neon tetra"))
                        .count());
    }

    @Test
    void exportsHistoryForOneAquariumInTheRequestedFormat() throws Exception {
        Aquarium aquarium = new Aquarium();
        aquarium.setName("History export aquarium");
        aquarium = aquariumRepository.save(aquarium);
        Event event = new Event("Water change", LocalDate.of(2026, 10, 1),
                "maintenance");
        event.setAquarium(aquarium);
        eventRepository.save(event);

        mockMvc.perform(get("/api/aquariums/{aquariumId}/history/export", aquarium.getId())
                        .param("format", "csv")
                        .param("allHistory", "true")
                        .param("sections", "events", "parameters", "information",
                                "maintenance", "co2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Water change")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("\"ID\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("\"ID Evento\""))));

        mockMvc.perform(get("/api/aquariums/{aquariumId}/history/export", aquarium.getId())
                        .param("format", "md")
                        .param("allHistory", "false")
                        .param("sections", "events"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsEditsListsCompletesAndDeletesRemindersThroughHttp() throws Exception {
        Aquarium aquarium = createAquarium("Task aquarium");
        int aquariumId = aquarium.getId();

        mockMvc.perform(post("/api/aquariums/{aquariumId}/tasks", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  ","description":"Invalid","actionDate":"2026-10-05","eventType":"info"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/aquariums/{aquariumId}/tasks", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unknown type","actionDate":"2026-10-05","eventType":"unknown"}
                                """))
                .andExpect(status().isBadRequest());

        String taskResponse = mockMvc.perform(post("/api/aquariums/{aquariumId}/tasks", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Change water","description":"Replace 20 percent","actionDate":"2026-10-05","eventType":"info"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Change water"))
                .andExpect(jsonPath("$.eventType").value("info"))
                .andReturn().getResponse().getContentAsString();
        int taskId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(taskResponse).get("id").asInt();

        mockMvc.perform(get("/api/aquariums/{aquariumId}/tasks", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Replace 20 percent"));

        mockMvc.perform(get("/api/aquariums/{aquariumId}/tasks/{taskId}", aquariumId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionDate").value("2026-10-05"));

        mockMvc.perform(get("/api/aquariums/{aquariumId}/tasks/calendar", aquariumId)
                        .param("start", "2026-10-01").param("end", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("reminder"));
        mockMvc.perform(get("/api/aquariums/{aquariumId}/tasks/calendar", aquariumId)
                        .param("start", "2026-10-01").param("end", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/tasks", aquariumId + 9999))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/tasks/{taskId}", aquariumId, taskId + 9999))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/aquariums/{aquariumId}/tasks/{taskId}", aquariumId, taskId + 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Missing","description":"","actionDate":"2026-10-06","eventType":"info"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/tasks/{taskId}", aquariumId, taskId + 9999))
                .andExpect(status().isNotFound());
        MockMultipartFile completion = new MockMultipartFile("completion", "", "application/json",
                ("{\"date\":\"" + LocalDate.now() + "\",\"infoType\":\"INFO\"}").getBytes());
        mockMvc.perform(multipart("/api/aquariums/{aquariumId}/tasks/{taskId}/complete", aquariumId, taskId + 9999)
                        .file(completion))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/aquariums/{aquariumId}/tasks/{taskId}", aquariumId, taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Water change","description":"Updated task","actionDate":"2026-10-06","eventType":"info"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Water change"));

        completion = new MockMultipartFile("completion", "", "application/json",
                ("{\"date\":\"" + LocalDate.now() + "\",\"infoType\":\"INFO\",\"status\":\"Good\","
                        + "\"observation\":\"Completed after review\"}").getBytes());
        mockMvc.perform(multipart("/api/aquariums/{aquariumId}/tasks/{taskId}/complete", aquariumId, taskId)
                        .file(completion))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(taskRepository.findByIdAndAquariumId(taskId, aquariumId).isEmpty());
        org.junit.jupiter.api.Assertions.assertFalse(eventRepository.findByAquariumIdAndStartDateAndType(
                aquariumId, LocalDate.now(), "info").isEmpty());

        String secondTaskResponse = mockMvc.perform(post("/api/aquariums/{aquariumId}/tasks", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Clean glass","description":"","actionDate":"2026-10-07","eventType":"parameters"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int secondTaskId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(secondTaskResponse).get("id").asInt();
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/tasks/{taskId}", aquariumId, secondTaskId))
                .andExpect(status().isNoContent());
    }

    @Test
    void exercisesParametersMaintenanceTreatmentAndPhotoHttpFlows() throws Exception {
        int aquariumId = createAquarium("Data aquarium").getId();

        mockMvc.perform(post("/api/aquariums/{aquariumId}/parameter-ranges", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":"ph","level1":5.5,"level2":6.0,"level3":7.0,
                                 "level4":8.0,"level5":8.5,"unit":""}
                                """))
                .andExpect(status().isCreated());
        long rangeId = rangeRepository.findByAquariumId(aquariumId).get(0).getId();
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameter-ranges", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parameter").value("ph"));
        mockMvc.perform(put("/api/aquariums/{aquariumId}/parameter-ranges/{rangeId}", aquariumId, rangeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":"ph","level1":5.0,"level2":6.0,"level3":7.0,
                                 "level4":8.0,"level5":9.0,"unit":"pH"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/parameters", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fecha":"2026-10-02","nh3":0.0,"no2":0.0,"no3":10.0,"ph":7.2,
                                 "kh":4.0,"gh":8.0,"temp":25.0}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/summary", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ph").value(7.2));
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ph").value(7.2));
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table/current-page", aquariumId))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table/total-pages", aquariumId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table/total-elements", aquariumId))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
        int parameterEventId = parametersRepository.findAllByAquariumId(aquariumId).get(0).getIdEvent();
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/{eventId}", aquariumId, parameterEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ph").value(7.2));
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameter-ranges", aquariumId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/events", aquariumId)
                        .param("start", "2026-10-01T00:00:00")
                        .param("end", "2026-10-31T23:59:59"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/events/co2/by-day/2026-10-02", aquariumId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/aquariums/{aquariumId}/events/information", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fecha":"2026-10-02","type":"General","status":"Healthy",
                                 "observation":"Integration observation"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/events/information/by-day/2026-10-02", aquariumId))
                .andExpect(status().isOk());
        int infoEventId = eventRepository.findByAquariumIdAndStartDateBetween(
                aquariumId, java.time.LocalDate.of(2026, 10, 2), java.time.LocalDate.of(2026, 10, 2))
                .stream().findFirst().orElseThrow().getId();
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/events/{eventId}", aquariumId, infoEventId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/parameter-ranges/{rangeId}", aquariumId, rangeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/maintenances", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fecha":"2026-10-01","watter":20,"prime":1.0,
                                 "glass":true,"pruning":"Trimmed plants","observations":"Weekly maintenance",
                                 "f1Sponge":true,"f1Perlon":false,"f1Mech":true,"f1Bio":true,"f1BioMech":false,
                                 "f2Sponge":false,"f2Perlon":true,"f2Mech":false,"f2Bio":true,"f2BioMech":true}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/maintenances/last", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.water").value(20))
                .andExpect(jsonPath("$.primeDose").value(1.0))
                .andExpect(jsonPath("$.filters[0].filterName").value("Filter 1"));
        int maintenanceEventId = maintenanceRepository.findLatestMaintenanceByAquariumId(aquariumId)
                .orElseThrow().getEvent().getId();
        mockMvc.perform(get("/api/aquariums/{aquariumId}/maintenances/{eventId}", aquariumId, maintenanceEventId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/treatments", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"CO2 mix","valor":"2.5","fecha":"2026-10-02"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/events/upcoming", aquariumId))
                .andExpect(status().isOk());

        MockMultipartFile image = new MockMultipartFile("foto", "aquarium.png", "image/png",
                new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/api/aquariums/{aquariumId}/photos", aquariumId).file(image)
                        .param("fecha", "2026-10-02"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/photos/by-day/2026-10-02", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/photos/last", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
        int photoId = photoRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/aquariums/{aquariumId}/photos/{photoId}/image", aquariumId, photoId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/photos/{photoId}", aquariumId, photoId))
                .andExpect(status().isNoContent());
    }

    @Test
    void managesNotificationsThroughHttp() throws Exception {
        int aquariumId = createAquarium("Notification aquarium").getId();
        String validNotification = """
                {"parameter":"ph","value":6,"active":true,"weekdaysList":["Mon","miércoles","invalid"]}
                """;

        mockMvc.perform(post("/api/aquariums/{aquariumId}/notifications", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":" ","value":0}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/notifications", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNotification))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/aquariums/{aquariumId}/notifications", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parameter").value("ph"))
                .andExpect(jsonPath("$[0].value").value(6));
        mockMvc.perform(get("/api/aquariums/{aquariumId}/notifications/active", aquariumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk());

        Long notificationId = notificationRepository.findByAquariumId(aquariumId).get(0).getId();
        mockMvc.perform(put("/api/aquariums/{aquariumId}/notifications/{notificationId}", aquariumId, notificationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":"no3","value":20,"active":false,"weekdays":127}
                                """))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/notifications/active", aquariumId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(delete("/api/aquariums/{aquariumId}/notifications/{notificationId}", aquariumId, notificationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/aquariums/{aquariumId}/notifications/{notificationId}", aquariumId, notificationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validNotification))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/notifications/telegram/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mapsScopedEndpointValidationAndNotFoundFailuresToHttpResponses() throws Exception {
        int aquariumId = createAquarium("Failure-path aquarium").getId();
        int missingAquariumId = aquariumId + 10000;

        mockMvc.perform(put("/api/aquariums/{aquariumId}", missingAquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Missing","type":"Freshwater","volume":50}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}", missingAquariumId))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/aquariums/{aquariumId}/filters", missingAquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Missing filter"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/notifications", missingAquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":"pH","value":7}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/notifications/{notificationId}", aquariumId, 987654L))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/aquariums/{aquariumId}/parameter-ranges", missingAquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":"pH","level1":4,"level2":5,"level3":6,
                                 "level4":7,"level5":8,"unit":"pH"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/aquariums/{aquariumId}/parameter-ranges/{rangeId}", aquariumId, 987654)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parameter":"pH","level1":4,"level2":5,"level3":6,
                                 "level4":7,"level5":8,"unit":"pH"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/parameter-ranges/{rangeId}", aquariumId, 987654))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table", aquariumId)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table/current-page", aquariumId)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/parameters/table/total-pages", aquariumId)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        MockMultipartFile invalidDateImage = new MockMultipartFile("foto", "aquarium.png", "image/png",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/aquariums/{aquariumId}/photos", aquariumId).file(invalidDateImage)
                        .param("fecha", "not-a-date"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/aquariums/{aquariumId}/photos/{photoId}/image", aquariumId, 987654))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/photos/{photoId}", aquariumId, 987654))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/aquariums/{aquariumId}/maintenances/{eventId}", aquariumId, 987654))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/aquariums/{aquariumId}/events/{eventId}", aquariumId, 987654))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/aquariums/{aquariumId}/treatments", aquariumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","valor":"1","fecha":"2026-10-02"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private Aquarium createAquarium(String name) {
        Aquarium aquarium = new Aquarium();
        aquarium.setName(name);
        return aquariumRepository.saveAndFlush(aquarium);
    }
}
