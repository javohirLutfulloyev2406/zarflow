package uz.zarflow.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Flushes {@link HumoFragmentBuffer} buckets that have gone quiet, merging their lines
 * back into {@link TelegramUpdateService} as one HUMO re-parse attempt. {@code
 * fixedDelay} (not {@code fixedRate}) means runs never overlap, so no distributed lock
 * is needed at this sub-second frequency -- unlike the daily/monthly schedulers. */
@Component
class HumoFragmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(HumoFragmentScheduler.class);

    private final HumoFragmentBuffer buffer;
    private final TelegramUpdateService updateService;

    HumoFragmentScheduler(HumoFragmentBuffer buffer, TelegramUpdateService updateService) {
        this.buffer = buffer;
        this.updateService = updateService;
    }

    @Scheduled(fixedDelay = 300)
    void flush() {
        for (HumoFragmentBuffer.ReadyBatch batch : buffer.drainReady()) {
            try {
                updateService.processBufferedHumoFragments(batch);
            } catch (Exception ex) {
                log.error("Failed to process buffered HUMO fragments for chat {}", batch.chatId(), ex);
            }
        }
    }

}
