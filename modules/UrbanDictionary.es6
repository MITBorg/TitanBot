class UrbanDictionary {
    constructor() {
        this.cache = {};
    }

    register() {

    }

    getHelp(event) {
        helper.respond(event, `Syntax: ${event.getArgs()[0]} [result #] (query)`);
    }

    getCommands() {
        return Java.to(['urbandictionary', 'urban', 'ub', 'ud'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        var AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        var Properties = Java.type('mitb.util.Properties');
        var StringHelper = Java.type('mitb.util.StringHelper');

        var args = commandEvent.getArgs();

        if (args.length == 0)
            return;

        if (args.length > 1 && !isNaN(args[0]) && isFinite(args[0])) {
            var res = args[0] - 1;
            var custom = true;
            args = Java.type('java.util.Arrays').copyOfRange(args, 1, args.length);
        } else {
            var res = 0;
            var custom = false;
        }

        var term = Java.from(args).join(' ').trim().toLowerCase();
        var result = this.cache[term + res];

        if (result != null) {
            helper.respond(commandEvent, result);
            return;
        }

        var url = `https://mashape-community-urban-dictionary.p.mashape.com/define?term=${encodeURIComponent(term)}`;
        var asyncHttpClient = new AsyncHttpClient();

        asyncHttpClient.prepareGet(url).addHeader("X-Mashape-Key", Properties.getValue("urbandict.api_key"))
            .addHeader("Accept", "text/plain")
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    var json = JSON.parse(response.getResponseBody());

                    if (json.list.length > res) {
                        var result = json.list[res];
                        var def = result.definition;

                        if (def.length() > 200) {
                            def = def.substr(0, 200 - 3) + "...";
                        }

                        def = `${def.replace(/\r?\n|\r/g, "")} [more at ${result.permalink}]`;
                        def = `${StringHelper.wrapBold(result.word)}: ${def} [by ${result.author} +${result.thumbs_up}/-${result.thumbs_down}]`;

                        this.cache[term + res] = def;
                        helper.respond(commandEvent, def);
                    } else {
                        var position = custom ? ` [at ${res + 1}]` : '';
                        helper.respond(commandEvent, `There are no entries for: ${term + position}`);
                    }
                }
            }));
    }
}

export default new UrbanDictionary();