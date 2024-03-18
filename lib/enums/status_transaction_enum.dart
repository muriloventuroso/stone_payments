// ignore_for_file: constant_identifier_names

/// StatusTransaction
///
/// This class is used to define the status of a transaction.
///
/// The status of a transaction is used in the following classes:
///
/// * [Transaction]
///
/// The following is the list of possible status:
///
/// * [UNKNOWN]
/// * [APPROVED]
/// * [DECLINED]
/// * [DECLINED_BY_CARD]
/// * [CANCELLED]
/// * [PARTIAL_APPROVED]
/// * [TECHNICAL_ERROR]
/// * [REJECTED]
/// * [WITH_ERROR]
/// * [PENDING]
/// * [REVERSED]
/// * [PENDING_REVERSAL]
/// * [TRANSACTION_WAITING_CARD]
/// * [TRANSACTION_WAITING_PASSWORD]
/// * [TRANSACTION_SENDING]
/// * [TRANSACTION_REMOVE_CARD]
/// * [TRANSACTION_CARD_REMOVED]
/// * [REVERSING_TRANSACTION_WITH_ERROR]
///
/// The status is used to determine the type of transaction.
///
/// For example, a transaction with the status [APPROVED] is a transaction
/// that was approved by the bank.
///
/// In the case of a transaction with the status [DECLINED], it is a transaction
/// that was not approved by the bank.
///
/// In the case of a transaction with the status [CANCELLED], it is a transaction
/// that was canceled by the user.
///
/// In the case of a transaction with the status [TECHNICAL_ERROR], it is a
/// transaction that was not completed due to a technical error.
///
/// In the case of a transaction with the status [WITH_ERROR], it is a transaction
/// that was not completed due to an error.
///
/// In the case of a transaction with the status [REJECTED], it is a transaction
/// that was rejected by the bank.
///
/// In the case of a transaction with the status [PARTIAL_APPROVED], it is a
/// transaction that was approved by the bank and has a partial value.
///
/// In the case of a transaction with the status [REVERSED], it is a transaction
/// that was automatically reversed by the bank.
///
/// In the case of a transaction with the status [PENDING], it is a transaction
/// that is in progress.
///
/// In the case of a transaction with the status [PENDING_REVERSAL],
class StatusTransaction {
  final String name;

  const StatusTransaction(this.name);
}
