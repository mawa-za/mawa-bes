package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.CardTerminalRequest;
import za.co.mawa.bes.entity.v2.CardTerminalEntity;
import za.co.mawa.bes.repository.v2.CardTerminalRepository;
import java.util.List;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class CardTerminalService {
    private final CardTerminalRepository repository;
    public List<CardTerminalEntity> list(boolean activeOnly){ return activeOnly?repository.findByActiveTrueOrderByNameAsc():repository.findAllByOrderByNameAsc(); }
    public CardTerminalEntity requireActive(String id){
        if(id==null||id.isBlank()) throw new IllegalArgumentException("Select the card terminal used for this card payment");
        CardTerminalEntity terminal=repository.findById(id.trim()).orElseThrow(()->new IllegalArgumentException("Card terminal not found"));
        if(!Boolean.TRUE.equals(terminal.getActive())) throw new IllegalStateException("The selected card terminal is inactive");
        return terminal;
    }
    @Transactional public CardTerminalEntity save(String id, CardTerminalRequest request){
        if(request==null||request.getCode()==null||request.getCode().isBlank()) throw new IllegalArgumentException("Terminal code is required");
        if(request.getName()==null||request.getName().isBlank()) throw new IllegalArgumentException("Terminal name is required");
        String code=request.getCode().trim().toUpperCase(Locale.ROOT);
        CardTerminalEntity entity=id==null?new CardTerminalEntity():repository.findById(id).orElseThrow(()->new IllegalArgumentException("Card terminal not found"));
        boolean duplicate = id == null
                ? repository.existsByCodeIgnoreCase(code)
                : repository.existsByCodeIgnoreCaseAndIdNot(code, id);
        if(duplicate) throw new IllegalArgumentException("Card terminal code already exists");
        entity.setCode(code); entity.setName(request.getName().trim());
        entity.setMerchantNumber(trim(request.getMerchantNumber())); entity.setLocationCode(trim(request.getLocationCode()));
        entity.setActive(request.getActive()==null||request.getActive()); entity.setUpdatedBy(trim(request.getChangedBy()));
        if(id==null) entity.setCreatedBy(trim(request.getChangedBy()));
        return repository.save(entity);
    }
    public void validateForPayment(String paymentMethod,String terminalId){ if("CARD".equalsIgnoreCase(paymentMethod)) requireActive(terminalId); }
    private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}
