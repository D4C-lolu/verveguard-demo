package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.dao.AccountDao;
import com.interswitch.verveguarddemo.dao.CardDao;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.ConflictException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.projections.CardValidationResult;
import com.interswitch.verveguarddemo.models.request.CreateCardRequest;
import com.interswitch.verveguarddemo.models.response.CardResponse;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CardService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "card_number", "card_type", "scheme",
            "expiry_month", "expiry_year", "card_status", "created_at", "updated_at",
            "account_number", "account_type", "currency", "balance"
    );
    private final CardDao cardDao;
    private final AccountDao accountDao;
    private final FraudDataService fraudDataService;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        Long merchantId = SecurityUtil.getCurrentUserId();
        String cardHash = DigestUtils.sha256Hex(request.cardNumber());

        CardValidationResult validation = cardDao.getCardCreationValidation(merchantId, cardHash)
                .orElseThrow(() -> new NotFoundException("Merchant not found"));

        if (!KycStatus.APPROVED.name().equals(validation.kycStatus()))
            throw new BadRequestException("Merchant must be KYC approved to add a card");

        if (validation.alreadyHasCard())
            throw new ConflictException("Merchant already has a card");

        if (validation.cardHashExists())
            throw new ConflictException("Card already exists in the system");

        validateCardExpiry(request.expiryMonth(), request.expiryYear());

        String maskedCardNumber = maskCardNumber(request.cardNumber());
        Long cardId = cardDao.insert(merchantId, maskedCardNumber, cardHash, request);

        accountDao.createForCard(cardId);

        return cardDao.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    @Cacheable(value = CacheId.Names.CARD, key = "'merchant:' + T(com.interswitch.verveguarddemo.util.SecurityUtil).getCurrentUserId()")
    public CardResponse getMyCard() {
        Long merchantId = SecurityUtil.getCurrentUserId();
        return cardDao.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    @Cacheable(value = CacheId.Names.CARD, key = "'number:' + #cardNumber")
    public CardResponse getCardByNumber(String cardNumber) {
        return cardDao.findByCardNumber(cardNumber)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    @Cacheable(value = CacheId.Names.CARDS, key = "'all:' + #page + ':' + #size + ':' + #sortField + ':' + #direction")
    public Page<CardResponse> getAllCards(int page, int size, String sortField, Sort.Direction direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortField))
            throw new BadRequestException("Invalid sort field: " + sortField);

        return cardDao.findAll(page, size, sortField, direction.name());
    }

    @Transactional
    @CacheEvict(value = CacheId.Names.CARD, key = "'merchant:' + T(com.interswitch.verveguarddemo.util.SecurityUtil).getCurrentUserId()")
    public void blockMyCard() {
        Long merchantId = SecurityUtil.getCurrentUserId();
        CardResponse card = cardDao.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (card.cardStatus() == CardStatus.EXPIRED)
            throw new BadRequestException("Card is already expired");

        String cardHash = cardDao.blockCard(card.id());
        fraudDataService.evict(cardHash);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void expireDueCards() {
        cardDao.expireDueCards();
    }

    private void validateCardExpiry(int expiryMonth, int expiryYear) {
        if (YearMonth.of(expiryYear, expiryMonth).isBefore(YearMonth.now()))
            throw new BadRequestException("Card expiry date is in the past");
    }

    public String maskCardNumber(String cardNumber) {
        return cardNumber.substring(0, 4) +
                "*".repeat(cardNumber.length() - 8) +
                cardNumber.substring(cardNumber.length() - 4);
    }
}