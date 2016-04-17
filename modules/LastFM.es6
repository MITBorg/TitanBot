import _ from 'lodash';
import moment from 'moment';

class LastFM {
    constructor() {
    }

    register() {
    }

    getHelp(event) {
        helper.respond(event, `Syntax: lastfm (user) (info|current|recent|topartists|topsongs overall/7day/1month/3month/6month/12month amt)`);
    }

    getCommands() {
        return Java.to(['lastfm', 'music', 'last'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        var args = Java.from(commandEvent.getArgs());

        var callerNick = Java.type('mitb.util.PIrcBotXHelper').getNick(commandEvent.getSource());

        var cachedUsername = this.fetchCachedUsername(callerNick);

        if (cachedUsername && args.length < 2)
            args.unshift(cachedUsername);
        else if (args.length > 0)
            this.updateCachedUsername(callerNick, args[0]);

        if (args.length == 1)
            args.push('current');

        if (args.length < 2)
            return;

        var AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        var Properties = Java.type('mitb.util.Properties');
        var StringHelper = Java.type('mitb.util.StringHelper');

        var user = args[0];
        var asyncHttpClient = new AsyncHttpClient();
        var template = null;
        var url = null;

        switch (args[1].toLowerCase()) {
            case 'info':
            case 'plays':
            case 'stats':
                template = (ret) => {
                    if (!ret.user) return;
                    var signUp = moment.unix(ret.user.registered['unixtime']).calendar();
                    return `${ret.user.name} has listened to ${StringHelper.wrapBold(ret.user.playcount)} songs since ${StringHelper.wrapBold(signUp)}.`;
                };
                url = `https://ws.audioscrobbler.com/2.0/?method=user.getInfo&user=${user}&api_key=${Properties.getValue('lastfm.api_key')}&format=json`;
                break;

            case 'current':
            case 'song':
            case 'recent':
                var amt;

                if (args.length < 3) {
                    amt = 1;
                } else if(!isNaN(args[2]) && isFinite(args[2])) {
                    amt = args[2];

                    if (amt < 1)
                        return;

                    if (amt > 5)
                        amt = 5;
                }

                template = (ret) => {
                    var track = ret.recenttracks.track[0];
                    if (!track) return;
                    var str = `${amt == 1 ? ((track['@attr'] && track['@attr'].nowplaying) ? 'Currently listening to' : 'Last listened to') : `Last ${amt} songs:`} `;

                    _(ret.recenttracks.track).each((track, i) => {
                        if (i > amt - 1) return;

                        str += StringHelper.wrapBold(`${track.artist['#text']} - ${track.name}`);

                        if (ret.recenttracks.track.length > 1 && i < ret.recenttracks.track.length - 1 && i < amt - 1) {
                            str += ' | ';
                        }
                    });

                    return str;
                };
                url = `https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=${user}&api_key=${Properties.getValue('lastfm.api_key')}&format=json&limit=${amt}`;
                break;

            case 'topartists':
            case 'toptracks':
            case 'topsongs':
                var time;

                if (args.length < 3) {
                    time = 'overall';
                } else {
                    time = args[2].toLowerCase();

                    if (time != '7day' && time != '1month' && time != '3month' && time != '6month' && time != '12month')
                        time = 'overall';
                }

                template = (ret) => {
                    var str = '';
                    var loop = [];
                    var fmt = (item) => `${StringHelper.wrapBold(item.name)} (${item.playcount})`;

                    if (args[1].toLowerCase() == 'topartists') {
                        if (!ret.topartists.artist[0]) return;

                        str = 'Top artists: ';
                        loop = ret.topartists.artist;
                    } else {
                        if (!ret.toptracks.track[0]) return;

                        str = 'Top songs: ';
                        loop = ret.toptracks.track;
                        fmt = (item) => `${StringHelper.wrapBold(`${item.name} - ${item.artist.name}`)} (${item.playcount})`;
                    }

                    _(loop).each((item, i) => {
                        str += fmt(item);

                        if (loop.length > 1 && i < loop.length - 1) {
                            str += ' | ';
                        }
                    });

                    return str;
                };

                url = `https://ws.audioscrobbler.com/2.0/?method=user.get${args[1].toLowerCase() == 'topartists' ? 'topartists' : 'toptracks'}&user=${user}&api_key=${Properties.getValue('lastfm.api_key')}&format=json&limit=5&period=${time}`;
                break;

            default:
                return;
        }

        asyncHttpClient.prepareGet(url)
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    var json = JSON.parse(response.getResponseBody());
                    var resp = template(json);

                    if (resp) {
                        helper.respond(commandEvent, resp);
                    }
                },
                onThrowable: (t) => {}
            }));
    }

    updateCachedUsername(nick, username) {
        try {
            var statement = Java.type('mitb.TitanBot').getDatabaseConnection().prepareStatement('INSERT OR REPLACE INTO lastfm (id, nick, username) VALUES ((SELECT id FROM lastfm WHERE nick = ?), ?, ?)');
            statement.setString(1, nick);
            statement.setString(2, nick);
            statement.setString(3, username);
            statement.executeUpdate();
        } catch(e) {
            e.printStackTrace();
        }
    }

    fetchCachedUsername(nick) {
        try {
            var statement = Java.type('mitb.TitanBot').getDatabaseConnection().prepareStatement('SELECT username FROM lastfm WHERE nick = ?');
            statement.setString(1, nick);
            var resultSet = statement.executeQuery();
            return resultSet.getString("username");
        } catch (e) {
            return null;
        }
    }
}

export default new LastFM();