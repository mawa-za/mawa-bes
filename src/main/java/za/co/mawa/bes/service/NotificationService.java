package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.NotificationDao;
import za.co.mawa.bes.dto.ContactDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.notification.NotificationCreate1Dto;
import za.co.mawa.bes.dto.notification.NotificationCreateDto;
import za.co.mawa.bes.dto.notification.NotificationDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.*;
import za.co.mawa.bes.dto.transaction.edit.TransactionEdit;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.notification.NotificationEntity;
import za.co.mawa.bes.entity.notification.NotificationLogEntity;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.repository.NotificationLogRepository;
import za.co.mawa.bes.repository.NotificationRepository;
import za.co.mawa.bes.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService implements NotificationDao {
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    NotificationLogRepository notificationLogRepository;
    @Autowired
    TransactionService transactionService;
    @Autowired
    FieldOptionService fieldOptionService;

    @Autowired
    PartnerService partnerService;

    @Override
    public String create(NotificationCreate1Dto notificationCreateDto) throws Exception {


        String sender = "";
        String receiver = "";

        String notificationId = "";

        PartnerDto partnerSenderDto = partnerService.get(notificationCreateDto.getProcessor());
        PartnerDto partnerReceiverDto = partnerService.get(notificationCreateDto.getRecipient());
        if (partnerReceiverDto.getId() != null) {
            receiver = partnerReceiverDto.getId();
        }

        if (partnerSenderDto.getId() != null) {
            sender = partnerSenderDto.getId();

        } else {
            sender = UserContext.getCurrentUserPartner();

        }

        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.NOTIFICATION);
        transactionCreateDto.setLocation(notificationCreateDto.getLocation());
        transactionCreateDto.setSubType(notificationCreateDto.getSubType());


        transactionCreateDto.setStatus(Status.NEW);

        TransactionDto transactionDto = transactionService.get(notificationCreateDto.getTransactionId());
        if (transactionDto != null) {
            transactionCreateDto.setCategory(transactionDto.getType());
            transactionCreateDto.setDescription(transactionDto.getStatus());
            transactionCreateDto.setSubDescription(transactionDto.getStatusReason());

        }

        TransactionDto transactionDto1 = transactionService.create(transactionCreateDto);

        if (transactionDto1.getId() != null) {
            notificationId = transactionDto1.getId();
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(notificationId);
            transactionPartnerDto.setFunction(PartnerFunction.PROCESSOR);
            transactionPartnerDto.setPartner(sender);
            transactionService.addPartner(transactionPartnerDto);


            TransactionPartnerDto transactionPartnerRecipientDto = new TransactionPartnerDto();
            transactionPartnerRecipientDto.setTransaction(notificationId);
            transactionPartnerRecipientDto.setFunction(PartnerFunction.RECIPIENT);
            transactionPartnerRecipientDto.setPartner(receiver);
            transactionService.addPartner(transactionPartnerRecipientDto);

            TransactionLinkDto transactionLinkDto = new TransactionLinkDto();
            transactionLinkDto.setTransaction1(transactionDto.getId());

            transactionLinkDto.setTransaction2(notificationId);
            transactionLinkDto.setType(TransactionType.NOTIFICATION);
            transactionLinkDto.setCreateBy(UserContext.getCurrentUserPartner());
            transactionService.addLink(transactionLinkDto);
        }


        return notificationId;
    }

    @Override
    public boolean send(String id) throws PartnerNotFoundException {


        return false;
    }


    @Override
    public void sendAll() {

    }

    @Override
    public NotificationDto notifications(String id) throws TransactionNotFound {


        TransactionDto transactionDto = transactionService.get(id);
        NotificationDto notificationDto = new NotificationDto();
        ArrayList<NotificationDto> notificationDtos = new ArrayList<>();
        NotificationDto notificationDto2 = new NotificationDto();
        if (transactionDto.getId() != null && !transactionDto.getType().equals(TransactionType.NOTIFICATION)) {
            List<TransactionLinkDto> transactionLinkDtoList = transactionService.getLinks(id);

            if (!transactionLinkDtoList.isEmpty()) {
                for (TransactionLinkDto transactionLinkDto : transactionLinkDtoList) {
                    notificationDto = get(transactionLinkDto.getTransaction2());
                    notificationDtos.add(notificationDto);
                }

            }

            notificationDto2.setTransactionId(transactionDto.getId());
            notificationDto2.setNotificationDtos(notificationDtos);

        }


        return notificationDto2;
    }

    @Override
    public NotificationDto get(String id) throws TransactionNotFound {

        TransactionDto transactionDto = transactionService.get(id);
        NotificationDto notificationDto = new NotificationDto();
        if (transactionDto.getId() != null && transactionDto.getType().equals(TransactionType.NOTIFICATION)) {
            notificationDto.setId(transactionDto.getId());
            notificationDto.setStatus(transactionDto.getStatus());
            notificationDto.setSubType(transactionDto.getSubType());
            notificationDto.setStatusReason(transactionDto.getStatusReason());
            notificationDto.setDescription(transactionDto.getDescription());
            notificationDto.setCategory(transactionDto.getCategory());
            notificationDto.setLocation(transactionDto.getLocation());
            TransactionLinkEntity transactionLinkEntity = transactionService.getTransaction(TransactionType.NOTIFICATION, id);

            if (transactionLinkEntity != null) {
                notificationDto.setTransactionId(transactionLinkEntity.getTransactionLinkPKEntity().getTransaction1());

            }

            List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id);

            if (!transactionPartnerDtoList.isEmpty()) {
                for (TransactionPartnerDto partnerDto : transactionPartnerDtoList) {
                    if (partnerDto.getFunction().equals(PartnerFunction.PROCESSOR)) {
                        notificationDto.setProcessor(partnerDto.getPartner());
                    }
                    if (partnerDto.getFunction().equals(PartnerFunction.RECIPIENT)) {
                        notificationDto.setRecipient(partnerDto.getPartner());
                    }

                }
            }


        }

        return notificationDto;
    }

    @Override
    public void read(String id) throws Exception {

        NotificationDto notificationDto = get(id);
        TransactionEditDto transactionEdit = new TransactionEditDto();
        if (notificationDto.getId() != null) {

            transactionEdit.setId(notificationDto.getId());
            transactionEdit.setStatus(Status.READ);
            transactionEdit.setStatusReason(StatusReason.SEEN);
            transactionService.edit(transactionEdit);
        }

    }

    @Override
    public NotificationDto getPartnerNotifications(String id) throws TransactionNotFound {


        ArrayList<NotificationDto> notificationDtos = new ArrayList<>();
        List<TransactionPartnerDto> transactionPartnerDtos = transactionService.getPartnerType(id, PartnerFunction.RECIPIENT);
        NotificationDto notificationDto2 = new NotificationDto();
        if (!transactionPartnerDtos.isEmpty()) {


            for (TransactionPartnerDto transactionPartnerDto : transactionPartnerDtos) {
                TransactionDto transactionDto = transactionService.get(transactionPartnerDto.getTransaction());
                if (transactionDto.getType().equals(TransactionType.NOTIFICATION)) {
                    NotificationDto notificationDto = get(transactionDto.getId());
                    if (notificationDto.getId() != null) {
                        notificationDtos.add(notificationDto);
                    }

                }
            }
            notificationDto2.setRecipient(id);
            notificationDto2.setNotificationDtos(notificationDtos);
        }

        return notificationDto2;
    }
}
