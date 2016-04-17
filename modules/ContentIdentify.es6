import _ from 'lodash';

class ContentIdentify {
    register() {
        helper.register(engine, 'onMessage', Java.type('mitb.event.events.MessageEvent'));
    }

    getHelp(event) {
        helper.respond(event, 'Retrieves some information about a pasted image.');
    }

    onMessage(event) {
        var AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        var Properties = Java.type('mitb.util.Properties');
        var asyncHttpClient = new AsyncHttpClient();

        var regex = /(?:(?:(?:https?):\/\/)|(?:www\.))(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,}))\.?)(?::\d{2,5})?(?:[/?#]\S+)\.(?:jpg|jpeg|png|gif)/ig;

        if (!regex.test(event.getSource().getMessage())) return;

        var img = event.getSource().getMessage().match(regex)[0];

        asyncHttpClient.preparePost('https://api.projectoxford.ai/vision/v1.0/describe?maxCandidates=1')
            .setBody(`{"url":"${img.replace('"', '%22')}"}`)
            .addHeader('Ocp-Apim-Subscription-Key', Properties.getValue('computer_vision.api_key'))
            .addHeader('Content-Type', 'application/json')
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    var json = JSON.parse(response.getResponseBody()).description;
                    if (!json) return;
                    json = json.captions[0];
                    if (!json) return;

                    var msg = 'The image pasted ';

                    if (json.confidence >= 0.5)
                        msg += `is probably of ${json.text}. `;
                    else if (json.confidence >= 0.8)
                        msg += `is of ${json.text}. `;
                    else
                        msg = '';

                    asyncHttpClient.preparePost('https://api.projectoxford.ai/vision/v1.0/analyze?visualFeatures=Adult')
                        .setBody(`{"url":"${img.replace('"', '%22')}"}`)
                        .addHeader('Ocp-Apim-Subscription-Key', Properties.getValue('computer_vision.api_key'))
                        .addHeader('Content-Type', 'application/json')
                        .execute(new com.ning.http.client.AsyncCompletionHandler({
                            onCompleted: (response) => {
                                var json = JSON.parse(response.getResponseBody()).adult;

                                if (!json) return;

                                if (json.isAdultContent || json.isRacyContent)
                                    msg += Java.type('mitb.util.StringHelper').wrapBold(`Warning: the image is ${Java.type('org.pircbotx.Colors').RED}NSFW${Java.type('org.pircbotx.Colors').NORMAL}.`);

                                if (msg != '')
                                    event.getSource().respondWith(msg);
                            },
                            onThrowable: (t) => {
                            }
                        }));
                },
                onThrowable: (t) => {}
            }));
    }
}

export default new ContentIdentify();