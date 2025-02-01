package application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DeepSeekChatbot {

    private static final String DEEPSEEK_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "YOUR_API_KEY";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Chatbot DeepSeek - Digite 'sair' para encerrar");

        while (true) {
            System.out.print("Você: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("sair")) {
                break;
            }

            String resposta = gerarResposta(userInput);
            System.out.println("Assistente: " + resposta);
        }
        scanner.close();
    }

    private static String gerarResposta(String prompt) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(DEEPSEEK_API_URL);
            
            // config em caso d timeout 
            RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(30 * 1000)
                .setConnectionRequestTimeout(30 * 1000)
                .setSocketTimeout(30 * 1000).build();
            httpPost.setConfig(config);

            // headers
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + API_KEY);
            httpPost.setHeader("HTTP-Referer", "https://github.com/hdmpedro/hdmpedro.github.io");
            httpPost.setHeader("X-Title", "Java Chatbot");

            // body da requestt
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen/qwen-turbo-2024-11-01");
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            requestBody.put("messages", messages);

            String jsonBody = new ObjectMapper().writeValueAsString(requestBody);
            httpPost.setEntity(new StringEntity(jsonBody));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);

                // verificar status
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    // tratamento específico para rate limit
                    if (statusCode == 429) {
                        return "Limite de requisições excedido. Aguarde alguns segundos.";
                    }
                    return "Erro na API: Código " + statusCode;
                }

                // parsiando com TypeReference
                Map<String, Object> responseMap = new ObjectMapper().readValue(
                    responseString, 
                    new TypeReference<Map<String, Object>>() {}
                );

                // verif se a erro na resposta
                if (responseMap.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                    return "Erro: " + error.get("message");
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices == null || choices.isEmpty()) {
                    return "Nenhuma resposta encontrada";
                }

                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                
                return content != null ? content : "Resposta vazia recebida";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro: " + e.getMessage();
        }
    }
}
