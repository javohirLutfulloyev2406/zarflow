package uz.zarflow.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import uz.zarflow.shared.config.LocaleConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProbeController.class)
@Import(LocaleConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundShapeIs404() throws Exception {
        mockMvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://zarflow.uz/problems/resource.not-found"))
                .andExpect(jsonPath("$.errorCode").value("resource.not-found"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.detail").value("So'ralgan ma'lumot topilmadi."));
    }

    @Test
    void conflictShapeIs409() throws Exception {
        mockMvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("probe.conflict"));
    }

    @Test
    void businessRuleShapeIs422() throws Exception {
        mockMvc.perform(get("/probe/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("probe.business-rule"));
    }

    @Test
    void validationFailureIs400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/probe/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void unhandledExceptionIs500AndHidesInternals() throws Exception {
        mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Kutilmagan xatolik yuz berdi. Birozdan keyin qayta urinib ko'ring."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sensitive internal detail"))))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void acceptLanguageRuReturnsRussianDetail() throws Exception {
        mockMvc.perform(get("/probe/not-found").header("Accept-Language", "ru"))
                .andExpect(jsonPath("$.detail").value("Запрошенные данные не найдены."));
    }

    @Test
    void clientSuppliedRequestIdIsEchoed() throws Exception {
        mockMvc.perform(get("/probe/not-found").header("X-Request-Id", "test-request-id-123"))
                .andExpect(header().string("X-Request-Id", "test-request-id-123"))
                .andExpect(jsonPath("$.requestId").value("test-request-id-123"));
    }

}
