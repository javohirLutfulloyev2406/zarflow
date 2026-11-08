package uz.zarflow.insight;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uz.zarflow.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/insights")
class InsightController {

    private final Insights insights;
    private final CurrentUser currentUser;

    InsightController(Insights insights, CurrentUser currentUser) {
        this.insights = insights;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<InsightResponse> list() {
        return insights.listActive(currentUser.requireUserId(), LocaleContextHolder.getLocale()).stream()
                .map(i -> new InsightResponse(i.id(), i.code(), i.icon(), i.text(), i.actionable(), i.createdAt()))
                .toList();
    }

    @PostMapping("/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void dismiss(@PathVariable UUID id) {
        insights.dismiss(currentUser.requireUserId(), id);
    }

    record InsightResponse(UUID id, InsightCode code, String icon, String text, boolean actionable, Instant createdAt) {
    }

}
