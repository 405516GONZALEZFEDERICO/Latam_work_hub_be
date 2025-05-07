package Latam.Latam.work.hub.services;

import org.springframework.stereotype.Service;


@Service
public interface MailService {
    void sendPaymentConfirmationEmail(String toEmail, String userName, String espacio, String fecha, double monto);
    void sendBookingNotificationToOwner(String ownerEmail, String ownerName, String spaceName, String userName, String startDate, String endDate, String startTime, String endTime);
    void sendBookingCompletedEmail(String userEmail, String userName, String spaceName);
    void sendBookingRefundConfirmationEmail(String userEmail, String userName, String spaceName, Double amount);

    void sendNewContractNotificationToOwner(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate, String monthlyAmount);
    void sendNewInvoiceNotification(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String dueDate, String totalAmount);
    void sendPaymentReminderEmail(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String dueDate, String totalAmount);
    void sendContractExpirationReminder(String tenantEmail, String tenantName, String spaceName, String endDate);
    void sendContractCancellationNotification(String tenantEmail, String tenantName, String spaceName, String startDate, String endDate);
    void sendOwnerContractCancellationNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate);
    void sendContractRenewalNotification(String tenantEmail, String tenantName, String spaceName, String months, String newEndDate);
    void sendOwnerContractRenewalNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String months, String newEndDate);
    void sendContractActivationNotification(String tenantEmail, String tenantName, String spaceName, String startDate, String endDate, String monthlyAmount);
    void sendOwnerContractActivationNotification(String ownerEmail, String ownerName, String spaceName, String tenantName, String startDate, String endDate);
    void sendRentalPaymentConfirmation(String tenantEmail, String tenantName, String spaceName, String invoiceNumber, String paymentDate, String amount);


    void sendUpcomingAutoRenewalNotification(String tenantEmail, String tenantName, String spaceName,
                                             String endDate, String renewalMonths);
    void sendAutoRenewalSetupNotification(String tenantEmail, String tenantName, String spaceName,
                                          String endDate, String renewalMonths, String status);
    void sendDepositRefundNotification(String tenantEmail, String tenantName, String spaceName, String refundAmount);

    void sendOverdueInvoiceNotification(String email, String name, String spaceName,
                                        String amount, String dueDate);

    void sendOwnerOverdueInvoiceNotification(String email, String ownerName,
                                             String spaceName, String tenantName,
                                             String amount, String dueDate);
}
