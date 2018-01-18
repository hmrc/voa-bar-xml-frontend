#!/bin/bash

echo "Applying migration CouncilTaxUpload"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /councilTaxUpload                       controllers.CouncilTaxUploadController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "councilTaxUpload.title = councilTaxUpload" >> ../conf/messages.en
echo "councilTaxUpload.heading = councilTaxUpload" >> ../conf/messages.en

echo "Moving test files from generated-test/ to test/"
rsync -avm --include='*.scala' -f 'hide,! */' ../generated-test/ ../test/
rm -rf ../generated-test/

echo "Migration CouncilTaxUpload completed"
