package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.constants.Roles;
import com.interswitch.verveguarddemo.models.request.CreateCardRequest;
import com.interswitch.verveguarddemo.models.response.CardResponse;
import com.interswitch.verveguarddemo.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Card Management", description = "Endpoints for issuing and managing payment cards")
@RestController
@RequestMapping("cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @Operation(
            summary = "Issue card (Merchant)",
            description = "Merchant issues a card for themselves. Requires MERCHANT role."
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("me")
    @PreAuthorize("hasRole('" + Roles.MERCHANT + "')")
    public CardResponse createCard(@RequestBody @Valid CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @Operation(
            summary = "Get my card (Merchant)",
            description = "Returns the authenticated merchant's card with account info. Requires MERCHANT role."
    )
    @GetMapping("me")
    @PreAuthorize("hasRole('" + Roles.MERCHANT + "')")
    public CardResponse getMyCard() {
        return cardService.getMyCard();
    }

    @Operation(
            summary = "Block my card (Merchant)",
            description = "Merchant blocks their own card. Requires MERCHANT role."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("me/block")
    @PreAuthorize("hasRole('" + Roles.MERCHANT + "')")
    public void blockMyCard() {
        cardService.blockMyCard();
    }

    @Operation(
            summary = "Get card by card number (Admin)",
            description = "Look up any card by card number. Requires ADMIN or SUPER_ADMIN role."
    )
    @GetMapping("{cardNumber}")
    @PreAuthorize("hasRole('" + Roles.ADMIN + "') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public CardResponse getCardByNumber(@PathVariable String cardNumber) {
        return cardService.getCardByNumber(cardNumber);
    }

    @Operation(
            summary = "List all cards (Admin)",
            description = "Paginated list of all cards with account info. Requires ADMIN or SUPER_ADMIN role."
    )
    @GetMapping
    @PreAuthorize("hasRole('" + Roles.ADMIN + "') or hasRole('" + Roles.SUPER_ADMIN + "')")
    public Page<CardResponse> getAllCards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return cardService.getAllCards(page, size, sortField, sortDirection);
    }
}