package ru.gavrilovegor519.hh_autoupdate_resume;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import ru.gavrilovegor519.hh_autoupdate_resume.dto.TokenDto;
import ru.gavrilovegor519.hh_autoupdate_resume.util.HhApiUtils;

import java.util.prefs.Preferences;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.enabled", matchIfMissing = true)
public class AutoUpdateResume {

    private final HhApiUtils hhApiUtils;
    private final String devResumeId;
    private final String sciResumeId;
    private final String devOpsResumeId;
    private final String tutorResumeId;
//    private final SendTelegramNotification sendTelegramNotification;

    private final Preferences preferences = Preferences.userRoot().node("hh-autoupdate-resume");

    private String accessToken = preferences.get("access_token", null);
    private String refreshToken = preferences.get("refresh_token", null);

    public AutoUpdateResume(
        HhApiUtils hhApiUtils,
        @Value("${ru.gavrilovegor519.hh-autoupdate-resume.devResumeId}") String devResumeId,
        @Value("${ru.gavrilovegor519.hh-autoupdate-resume.sciResumeId}") String sciResumeId,
        @Value("${ru.gavrilovegor519.hh-autoupdate-resume.devOpsResumeId}") String devOpsResumeId,
        @Value("${ru.gavrilovegor519.hh-autoupdate-resume.tutorResumeId}") String tutorResumeId
    ) {
        this.hhApiUtils = hhApiUtils;
//        this.sendTelegramNotification = sendTelegramNotification;
        this.devResumeId = devResumeId;
        this.sciResumeId = sciResumeId;
        this.devOpsResumeId = devOpsResumeId;
        this.tutorResumeId = tutorResumeId;
    }

    @Scheduled(fixedRate = 14410000)
    public void updateResume() {
        if (accessToken != null && refreshToken != null) {
            try {
                updateResumeInternal();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(403))) {
                    updateTokens(false);
                    updateResumeInternal();
                }
            }
        } else {
            updateTokens(true);
            updateResumeInternal();
        }
    }

    private void updateResumeInternal() {
        try {
            hhApiUtils.updateResume(sciResumeId, accessToken);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        try {
            hhApiUtils.updateResume(devResumeId, accessToken);
//            sendTelegramNotification.send("Резюме обновлено");
        } catch (Exception e) {
            System.out.println(e.getMessage());
//            if (!e.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(403))) {
//                sendTelegramNotification.send("Ошибка обновления резюме: " + e.getMessage());
//            }
//            throw e;
        }
        try {
            hhApiUtils.updateResume(devOpsResumeId, accessToken);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        try {
            hhApiUtils.updateResume(tutorResumeId, accessToken);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateTokens(boolean isInitial) {
        try {
            if (isInitial) {
                updateTokensInPreferences(hhApiUtils.getInitialToken());
            } else {
                updateTokensInPreferences(hhApiUtils.getNewToken(refreshToken));
            }
//            sendTelegramNotification.send("Токены обновлены");
        } catch (Exception e) {
//            sendTelegramNotification.send("Ошибка обновления токенов: " + e.getMessage());
        }
    }

    private void updateTokensInPreferences(TokenDto tokenDto) {
        if (tokenDto != null && !tokenDto.access_token().isEmpty() &&
                !tokenDto.refresh_token().isEmpty()) {
            preferences.put("access_token", tokenDto.access_token());
            preferences.put("refresh_token", tokenDto.refresh_token());
            accessToken = preferences.get("access_token", null);
            refreshToken = preferences.get("refresh_token", null);
        }
    }

}
