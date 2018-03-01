#!/bin/bash

echo "Applying migration ReportStatus"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /reportStatus                       controllers.ReportStatusController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "reportStatus.title = reportStatus" >> ../conf/messages.en
echo "reportStatus.heading = reportStatus" >> ../conf/messages.en

echo "Moving test files from generated-test/ to test/"
rsync -avm --include='*.scala' -f 'hide,! */' ../generated-test/ ../test/
rm -rf ../generated-test/

echo "Migration ReportStatus completed"
