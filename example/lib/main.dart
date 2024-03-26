import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:stone_payments/enums/item_print_type_enum.dart';
import 'package:stone_payments/enums/status_transaction_enum.dart';
import 'package:stone_payments/enums/type_owner_print_enum.dart';
import 'package:stone_payments/enums/type_transaction_enum.dart';
import 'package:stone_payments/models/item_print_model.dart';
import 'package:stone_payments/stone_payments.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final stonePaymentsPlugin = StonePayments();
  await stonePaymentsPlugin.activateStone(
      appName: 'My App',
      stoneCode: '12345678',
      stoneKeys: [
        "e49da9d6-79dc-4316-957a-b63d0a7b21a8",
        "6947bd9d-2cff-4ee7-b302-a3ed642b20f5"
      ]);

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final stonePaymentsPlugin = StonePayments();
  String text = 'Running';
  late StreamSubscription<StatusTransaction> listen;
  String transactionStored = "";
  String qrcode = "";

  @override
  void initState() {
    listen = stonePaymentsPlugin.onMessageListener((message) {
      setState(() {
        text = message.name;
      });
    });
    stonePaymentsPlugin.onTransactionListener((transaction) {
      transactionStored = json.decode(transaction)["initiatorTransactionKey"];
    });

    stonePaymentsPlugin.onQRCodeListener((qrcode) {
      setState(() {
        this.qrcode = qrcode;
      });
    });
    super.initState();
  }

  Image imageFromBase64String(String base64String) {
    return Image.memory(base64.decode(base64String.split(',').last));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: SizedBox(
          width: double.infinity,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(text),
              ElevatedButton(
                onPressed: () async {
                  if (listen.isPaused) {
                    listen.resume();
                  }
                  try {
                    await stonePaymentsPlugin.payment(
                      value: 5,
                      typeTransaction: TypeTransactionEnum.debit,
                      printReceipt: true,
                    );
                  } catch (e) {
                    listen.pause();
                    setState(() {
                      text = "Falha no pagamento";
                    });
                  }
                },
                child: const Text('Comprar R\$5,00'),
              ),
              ElevatedButton(
                onPressed: () async {
                  if (listen.isPaused) {
                    listen.resume();
                  }
                  try {
                    await stonePaymentsPlugin.payment(
                      value: 5,
                      typeTransaction: TypeTransactionEnum.pix,
                      printReceipt: true,
                    );
                  } catch (e) {
                    listen.pause();
                    setState(() {
                      text = "Falha no pagamento";
                    });
                  }
                },
                child: const Text('Comprar no Pix'),
              ),
              ElevatedButton(
                onPressed: () async {
                  if (listen.isPaused) {
                    listen.resume();
                  }
                  try {
                    await stonePaymentsPlugin.abortPayment();
                  } catch (e) {
                    listen.pause();
                    setState(() {
                      text = "Falha no pagamento";
                    });
                  }
                },
                child: const Text('Abort'),
              ),
              ElevatedButton(
                onPressed: () async {
                  if (listen.isPaused) {
                    listen.resume();
                  }
                  print(transactionStored);
                  try {
                    await stonePaymentsPlugin.cancel(
                        transactionId: transactionStored);
                  } catch (e) {
                    listen.pause();
                    setState(() {
                      text = "Falha no pagamento";
                    });
                  }
                },
                child: const Text('Cancel'),
              ),
              if (qrcode != "") imageFromBase64String(qrcode),
              ElevatedButton(
                onPressed: () async {
                  try {
                    var byteData =
                        await rootBundle.load('assets/flutter5786.png');
                    var imgBase64 = base64Encode(byteData.buffer.asUint8List());

                    var items = [
                      const ItemPrintModel(
                        type: ItemPrintTypeEnum.text,
                        data: 'Teste Título',
                      ),
                      const ItemPrintModel(
                        type: ItemPrintTypeEnum.text,
                        data: 'Teste Subtítulo',
                      ),
                      ItemPrintModel(
                        type: ItemPrintTypeEnum.image,
                        data: imgBase64,
                      ),
                    ];

                    await stonePaymentsPlugin.print(items);
                  } catch (e) {
                    setState(() {
                      text = "Falha na impressão";
                    });
                  }
                },
                child: const Text('Imprimir'),
              ),
              ElevatedButton(
                onPressed: () async {
                  try {
                    await stonePaymentsPlugin
                        .printReceipt(TypeOwnerPrintEnum.merchant);
                  } catch (e) {
                    setState(() {
                      text = "Falha na impressão";
                    });
                  }
                },
                child: const Text('Imprimir Via Loja'),
              ),
              ElevatedButton(
                onPressed: () async {
                  try {
                    await stonePaymentsPlugin
                        .printReceipt(TypeOwnerPrintEnum.client);
                  } catch (e) {
                    setState(() {
                      text = "Falha na impressão";
                    });
                  }
                },
                child: const Text('Imprimir Via Cliente'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
