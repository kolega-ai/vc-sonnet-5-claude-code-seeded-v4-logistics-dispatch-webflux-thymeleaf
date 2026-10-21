package com.fielddispatch.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders the shipment-delay wording the carrier desk publishes against the
 * current depot context, so the dispatcher signs off on the exact customer
 * message before it is filed as a notice on the affected jobs.
 */
@Service
public class DelayNoticeService {

    public String render(String carrier, String wording) {
        try {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
            Template template = new Template("delayNotice", wording, configuration);
            Map<String, Object> model = new HashMap<>();
            model.put("carrier", carrier == null ? "" : carrier);
            model.put("depot", "Central depot");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalArgumentException("Delay wording could not be rendered", e);
        }
    }
}
