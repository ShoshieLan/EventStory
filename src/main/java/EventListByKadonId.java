import com.fasterxml.jackson.databind.*;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import spark.ModelAndView;
import spark.Request;
import spark.template.velocity.VelocityTemplateEngine;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static spark.Spark.get;
import static spark.Spark.port;


/**
 * Created by slan on 6/8/2017.
 */
public class EventListByKadonId {

    public static void main(String args[]) throws Exception {
 port(8020);

        String layout = "templates/layout.vtl";

        CloseableHttpClient httpclient = HttpClients.createDefault();

        get("/home", (request, response) -> {

            HashMap maps = new HashMap();
            maps.put("template", "templates/home.vtl");
            maps.put("id", request.queryParams("id"));
            return  new ModelAndView(maps, layout);
        }, new VelocityTemplateEngine());



            get("/event", (req, res) -> {
            ArrayList<Event> eventList = new ArrayList<>();

            Map<String, Object> model = new HashMap<>();

            String id = req.queryParams("id");
            String format = req.queryParams("format");


            HttpGet httpget = new HttpGet("http://oorah-admire04:9200/newindex/_search?q=KadonID:" + id + "");

            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            final JsonNode arrNode = new ObjectMapper().readTree(httpclient.execute(httpget, responseHandler)).get("hits").get("hits");

            Event event = new EventBuilder().createEvent();

            if (arrNode.isArray()) {

                for (final JsonNode objNode : arrNode) {

                    event = new EventBuilder().setKadonId(Integer.parseInt(id)).createEvent();
                    if (objNode.get("_source").get("@timestamp") != null) {
                        String Gettext = String.valueOf(objNode.get("_source").get("@timestamp").asText());
                        Date date = formatDate(Gettext);
                        event.setDateTime(date);
                    }
                    if (objNode.get("_source").get("EventName") != null){
                        String eventName = ("Event Name: " + objNode.get("_source").get("EventName").asText());
                        event.setEventName(eventName);
                    }

                    if (objNode.get("_source").get("NewValue") != null){
                        String newValue = ("New Value: " + objNode.get("_source").get("NewValue").asText());
                        event.setNewValue(newValue);
                    }

                    if (objNode.get("_source").get("OldValue") != null){
                        String oldValue = ("Old Value: " + objNode.get("_source").get("OldValue").asText());
                        event.setNewValue(oldValue);
                    }

                    if (objNode.get("_source").get("CarType") != null){
                        String carType = ("Car Type: " + objNode.get("_source").get("CarType").asText());
                        event.setNewValue(carType);
                    }
                    if (objNode.get("_source").get("Stage") != null){
                        String stage = ("Stage: " + objNode.get("_source").get("Stage").asText());
                        event.setNewValue(stage);
                    }

                    eventList.add(event);

                }
            }

            Collections.sort(eventList, new Comparator<Event>() {
                public int compare(Event one, Event two) {
                    Date thisTime = one.getDateTime();
                    Date anotherTime = two.getDateTime();
                    return thisTime.compareTo(anotherTime) < 0 ? -1 : (thisTime == anotherTime ? 0 : 1);
                }});

if(shouldReturnJson(req)){

    String json = new Gson().toJson(eventList);
    return json;
}
else {

    model.put("template", "templates/event.vtl");

    model.put("eventList", eventList);

    return new VelocityTemplateEngine().render(new ModelAndView(model,"templates/layout.vtl" ));
}  });}



    public static java.util.Date formatDate(String date) {
        Date response = new Date();
        try {

            String finaltext = date.substring(1);
            String last = finaltext.replace("T", " ");
            String really = last.replace("Z", " ");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            response = formatter.parse(really);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return response;
    }

    private static boolean shouldReturnJson(Request request){
        String format = request.queryParams("format");
        return format != null && format.contains("json");
    }

}


