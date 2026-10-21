package com.fielddispatch.web;

import com.fielddispatch.security.AppUserPrincipal;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.AuthorizationService;
import com.fielddispatch.service.CarrierIntegrationService;
import com.fielddispatch.service.DelayNoticeService;
import com.fielddispatch.service.RateCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Partner desk console: haulage carriers are onboarded from their handover
 * sheet and their nightly rate-card drop is applied to the service-type
 * catalogue. Both actions are restricted to dispatch staff, matching the
 * board's region-scoped staff gate.
 */
@Controller
@RequestMapping("/dispatch/partners")
@RequiredArgsConstructor
public class DispatchPartnersController {

    private final CurrentUser currentUser;
    private final AuthorizationService authorizationService;
    private final CarrierIntegrationService carrierIntegrationService;
    private final RateCardService rateCardService;
    private final DelayNoticeService delayNoticeService;

    @GetMapping
    public Mono<String> console() {
        return currentUser.get().flatMap(user -> requireStaff(user)).thenReturn("dispatcher/partners");
    }

    @PostMapping("/carriers")
    public Mono<String> onboardCarrier(@RequestPart("sheet") FilePart sheet) {
        return currentUser.get()
                .flatMap(user -> requireStaff(user).then(carrierIntegrationService.onboardCarrier(user, sheet)))
                .thenReturn("redirect:/dispatch/partners?carrier")
                .onErrorResume(ex -> Mono.just("redirect:/dispatch/partners?error"));
    }

    @PostMapping("/rate-cards")
    public Mono<String> applyRateCard(@RequestPart("card") FilePart card) {
        return currentUser.get()
                .flatMap(user -> requireStaff(user).then(rateCardService.applyRateCard(user, card)))
                .thenReturn("redirect:/dispatch/partners?rateCard")
                .onErrorResume(ex -> Mono.just("redirect:/dispatch/partners?error"));
    }

    @PostMapping("/notices/delay")
    @ResponseBody
    public Mono<String> delayNotice(@RequestParam String carrier, @RequestParam String wording) {
        return currentUser.get()
                .flatMap(user -> requireStaff(user)
                        .then(Mono.fromCallable(() -> delayNoticeService.render(carrier, wording))));
    }

    @GetMapping("/catalog/carriers")
    @ResponseBody
    public Mono<String> carrierCatalog(@RequestParam String region, @RequestParam(defaultValue = "") String name) {
        return currentUser.get().flatMap(user -> requireStaff(user).then(Mono.fromCallable(() -> {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(getClass().getResourceAsStream("/carriers/catalog.xml"));
            XPathExpression expression = XPathFactory.newInstance().newXPath().compile(
                    "//carrier[region='" + region + "' and contains(name,'" + name + "')]/code");
            NodeList nodes = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);
            StringBuilder matches = new StringBuilder();
            for (int i = 0; i < nodes.getLength(); i++) {
                if (i > 0) {
                    matches.append(',');
                }
                matches.append(nodes.item(i).getTextContent());
            }
            return matches.toString();
        })));
    }

    private Mono<Void> requireStaff(AppUserPrincipal user) {
        return authorizationService.isStaff(user) ? Mono.empty() : Mono.error(AuthorizationService.forbidden());
    }
}
