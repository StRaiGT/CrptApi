import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final long timeUnitNumber = 1;
    private Date lastResetTime = new Date();
    private final HttpClient httpClient = HttpClients.createDefault();
    private final String API_URL_CREATE = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    private void checkRequestCounter() throws InterruptedException {
        while (requestCounter.get() >= requestLimit) {
            long currentTime = new Date().getTime();
            long timePassed = currentTime - lastResetTime.getTime();

            if (timePassed >= timeUnit.toMillis(timeUnitNumber)) {
                requestCounter.getAndSet(0);
                lastResetTime = new Date(currentTime);
            } else {
                wait(timeUnit.toMillis(timeUnitNumber) - timePassed);
            }
        }
    }

    private void sendRequest(Document document, String signature) throws IOException {
        HttpPost httpPost = new HttpPost(API_URL_CREATE);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Signature", signature);
        httpPost.setEntity(new StringEntity(
                objectMapper.writeValueAsString(document)
        ));

        httpClient.execute(httpPost);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        synchronized (this) {
            checkRequestCounter();
            sendRequest(document, signature);
            requestCounter.incrementAndGet();
        }
    }

    public enum DocType {
        LP_INTRODUCE_GOODS
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private DocType doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }
}
