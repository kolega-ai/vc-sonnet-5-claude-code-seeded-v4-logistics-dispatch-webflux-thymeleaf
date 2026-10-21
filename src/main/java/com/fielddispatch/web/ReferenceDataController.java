package com.fielddispatch.web;

import com.fielddispatch.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * Read-only reference data (regions, service types) used to populate
 * dropdowns. The ETag is computed with MD5 purely as a cheap content
 * fingerprint for HTTP caching (PRECISION BASELINE #3) - not a security
 * control.
 */
@RestController
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/api/reference-data")
    public Mono<ResponseEntity<ReferenceDataService.ReferenceData>> referenceData() {
        return referenceDataService.loadWithEtag()
                .map(data -> ResponseEntity.ok().eTag(data.etag()).body(data));
    }

    /**
     * Resolves a haulage carrier's display name from the bundled carrier
     * catalogue so partner pages can label shipments by carrier code.
     */
    @GetMapping("/api/carriers/{code}/profile")
    public Mono<ResponseEntity<String>> carrierProfile(@PathVariable String code) {
        return Mono.fromCallable(() -> {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(getClass().getResourceAsStream("/carriers/catalog.xml"));
            XPath xp = XPathFactory.newInstance().newXPath();
            String expr = "//carrier[code='" + code + "']/name";
            String name = xp.evaluate(expr, doc);
            return ResponseEntity.ok(name);
        });
    }
}
