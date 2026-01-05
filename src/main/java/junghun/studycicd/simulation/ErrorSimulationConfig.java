package junghun.studycicd.simulation;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ErrorSimulationConfig {

    // 주기적 에러 발생 설정
    private final AtomicBoolean periodicErrorEnabled = new AtomicBoolean(false);
    private final AtomicInteger periodicErrorIntervalSeconds = new AtomicInteger(30);
    private final AtomicInteger periodicErrorRate = new AtomicInteger(20); // 20%

    // API 에러 발생 설정
    private final AtomicBoolean apiErrorEnabled = new AtomicBoolean(false);
    private final AtomicInteger apiErrorRate = new AtomicInteger(10); // 10%

    // 에러 타입 분포 가중치
    private final AtomicInteger internalErrorWeight = new AtomicInteger(30);
    private final AtomicInteger badRequestWeight = new AtomicInteger(15);
    private final AtomicInteger notFoundWeight = new AtomicInteger(10);
    private final AtomicInteger serviceUnavailableWeight = new AtomicInteger(15);
    private final AtomicInteger timeoutWeight = new AtomicInteger(10);
    private final AtomicInteger databaseErrorWeight = new AtomicInteger(10);
    private final AtomicInteger externalApiErrorWeight = new AtomicInteger(5);
    private final AtomicInteger memoryErrorWeight = new AtomicInteger(5);

    // Getters and Setters
    public boolean isPeriodicErrorEnabled() {
        return periodicErrorEnabled.get();
    }

    public void setPeriodicErrorEnabled(boolean enabled) {
        periodicErrorEnabled.set(enabled);
    }

    public int getPeriodicErrorIntervalSeconds() {
        return periodicErrorIntervalSeconds.get();
    }

    public void setPeriodicErrorIntervalSeconds(int seconds) {
        periodicErrorIntervalSeconds.set(seconds);
    }

    public int getPeriodicErrorRate() {
        return periodicErrorRate.get();
    }

    public void setPeriodicErrorRate(int rate) {
        periodicErrorRate.set(Math.max(0, Math.min(100, rate)));
    }

    public boolean isApiErrorEnabled() {
        return apiErrorEnabled.get();
    }

    public void setApiErrorEnabled(boolean enabled) {
        apiErrorEnabled.set(enabled);
    }

    public int getApiErrorRate() {
        return apiErrorRate.get();
    }

    public void setApiErrorRate(int rate) {
        apiErrorRate.set(Math.max(0, Math.min(100, rate)));
    }

    public int getInternalErrorWeight() {
        return internalErrorWeight.get();
    }

    public void setInternalErrorWeight(int weight) {
        internalErrorWeight.set(Math.max(0, weight));
    }

    public int getBadRequestWeight() {
        return badRequestWeight.get();
    }

    public void setBadRequestWeight(int weight) {
        badRequestWeight.set(Math.max(0, weight));
    }

    public int getNotFoundWeight() {
        return notFoundWeight.get();
    }

    public void setNotFoundWeight(int weight) {
        notFoundWeight.set(Math.max(0, weight));
    }

    public int getServiceUnavailableWeight() {
        return serviceUnavailableWeight.get();
    }

    public void setServiceUnavailableWeight(int weight) {
        serviceUnavailableWeight.set(Math.max(0, weight));
    }

    public int getTimeoutWeight() {
        return timeoutWeight.get();
    }

    public void setTimeoutWeight(int weight) {
        timeoutWeight.set(Math.max(0, weight));
    }

    public int getDatabaseErrorWeight() {
        return databaseErrorWeight.get();
    }

    public void setDatabaseErrorWeight(int weight) {
        databaseErrorWeight.set(Math.max(0, weight));
    }

    public int getExternalApiErrorWeight() {
        return externalApiErrorWeight.get();
    }

    public void setExternalApiErrorWeight(int weight) {
        externalApiErrorWeight.set(Math.max(0, weight));
    }

    public int getMemoryErrorWeight() {
        return memoryErrorWeight.get();
    }

    public void setMemoryErrorWeight(int weight) {
        memoryErrorWeight.set(Math.max(0, weight));
    }

    public int getTotalWeight() {
        return internalErrorWeight.get() + badRequestWeight.get() + notFoundWeight.get() +
                serviceUnavailableWeight.get() + timeoutWeight.get() + databaseErrorWeight.get() +
                externalApiErrorWeight.get() + memoryErrorWeight.get();
    }
}
