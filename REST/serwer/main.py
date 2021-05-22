from flask import Flask, render_template, request
import requests
import datetime

app = Flask(__name__)


# https://megavangelicals.com/sundays.json
# https://github.com/egno/work-calendar
# https://www.gov.uk/bank-holidays.json

@app.route('/', methods=['GET'])
def get_main():
    return render_template('app.html')


# popracować nad tym
def between_the_dates(beginning_year, beginning_month, beginning_day, ending_year, ending_month, ending_day, date):
    split_date = date.split("-", 3)

    year = int(split_date[0])
    month = int(split_date[1])
    day = int(split_date[2])

    if not is_before(beginning_year, beginning_month, beginning_day, year, month, day):
        return False

    if not is_before(year, month, day, ending_year, ending_month, ending_day):
        return False

    return True


# popracować nad tym
def get_english(beginning_year, beginning_month, beginning_day, ending_year, ending_month, ending_day):
    endpoint = "https://www.gov.uk/bank-holidays.json"
    main_result = requests.get(endpoint).json()["england-and-wales"]["events"]
    holidays_list = ""
    for result in main_result:
        if between_the_dates(beginning_year, beginning_month, beginning_day, ending_year, ending_month, ending_day,
                             result["date"]):
            holidays_list += result["title"] + " " + result["date"] + ", "
    if len(holidays_list) == 0:
        return "None english holidays in this time"

    return holidays_list


def get_catholic_info(json_info):
    result = json_info["date"] + ": "
    celebrations = json_info["celebrations"]

    for celebration in celebrations:
        result += celebration["title"] + ", "

    return result


# popracować nad tym
def get_catholic(beginning_year, beginning_month, beginning_day, ending_year, ending_month, ending_day):
    endpoint = "http://calapi.inadiutorium.cz/api/v0/en/calendars/default"
    endpoint += "/" + str(beginning_year) + "/" + str(beginning_month) + "/" + str(beginning_day)
    result = get_catholic_info(requests.get(endpoint).json())

    date = datetime.datetime(beginning_year, beginning_month, beginning_day, 1, 1, 1)
    ending_date = datetime.datetime(ending_year, ending_month, ending_day, 1, 1, 1)

    while not date == ending_date:
        date += datetime.timedelta(days=1)
        endpoint = "http://calapi.inadiutorium.cz/api/v0/en/calendars/default"
        endpoint += "/" + str(date.year) + "/" + str(date.month) + "/" + str(date.day)
        result += get_catholic_info(requests.get(endpoint).json())

    return result


def is_before(beginning_year, beginning_month, beginning_day, ending_year, ending_month, ending_day):
    if beginning_year > ending_year:
        return False
    if ending_year > beginning_year:
        return True

    if beginning_month > ending_month:
        return False
    if ending_month > beginning_month:
        return True

    if beginning_day > ending_day:
        return False

    return True


# popracować nad tym
@app.route('/', methods=['POST'])
def post_main():
    beginning_date = request.form['beginning_date']
    ending_date = request.form['ending_date']
    if len(beginning_date) == 0 or len(ending_date) == 0:
        return render_template('app.html', values="Fill the dates please!")

    split_beginning_date = beginning_date.split("-", 3)
    split_ending_date = ending_date.split("-", 3)

    beginning_year = int(split_beginning_date[0])
    beginning_month = int(split_beginning_date[1])
    beginning_day = int(split_beginning_date[2])

    ending_year = int(split_ending_date[0])
    ending_month = int(split_ending_date[1])
    ending_day = int(split_ending_date[2])

    if not is_before(beginning_year, beginning_month, beginning_day, ending_year, ending_month, ending_day):
        return render_template('app.html', values="The dates are incorrect!")

    operation = request.form['operation']

    if operation == "catholic":
        return render_template('app.html',
                               catholic=get_catholic(beginning_year, beginning_month, beginning_day, ending_year,
                                                     ending_month, ending_day))
    elif operation == "english":
        return render_template('app.html',
                               english=get_english(beginning_year, beginning_month, beginning_day, ending_year,
                                                   ending_month, ending_day))
    else:
        english = "[ENGLISH HOLIDAYS]: " + get_english(beginning_year, beginning_month, beginning_day, ending_year,
                                                       ending_month, ending_day) + "\n"
        catholic = "[CATHOLIC HOLIDAYS]: " + get_catholic(beginning_year, beginning_month, beginning_day, ending_year,
                                                          ending_month, ending_day)
        return render_template('app.html', english=english, catholic=catholic)


@app.route('/bydate', methods=['GET'])
def get_date_page():
    return render_template('namedaydate.html')


@app.route('/bydate', methods=['POST'])
def get_date():
    name = request.form['name']

    if len(name) == 0:
        return render_template('namedaydate.html', dates="Name cannot be empty", other_names="Name cannot be empty")

    if not name.isalpha():
        return render_template('namedaydate.html', dates="Names contain only letters",
                               other_names="Names contain only letters")

    country = request.form['country']
    endpoint = "https://api.abalin.net/getdate"
    payload = {'country': country, 'name': name.capitalize()}
    main_result = requests.get(endpoint, params=payload).json()['results']
    dates = ""
    other_names = ""
    for result in main_result:
        dates += str(result['day']) + "." + str(result['month']) + " and "
        other_names += result['name']
    size = len(dates)
    result_dates = dates[:size - 4]

    if len(dates) == 0:
        return render_template('namedaydate.html', dates="No dates reserved for this name",
                               other_names="No dates reserved for this name")

    if len(other_names) == 0:
        return render_template('namedaydate.html', dates=result_dates,
                               other_names="No other name celebrates at this day")

    return render_template('namedaydate.html', dates=result_dates, other_names=other_names)


@app.route('/namesday', methods=['GET'])
def get_name_page():
    return render_template('namesday.html')


@app.route('/namesday', methods=['POST'])
def get_names():
    country = request.form['country']
    date = request.form['date']
    if len(date) == 0:
        return render_template('namesday.html', names="Select the date please!")
    endpoint = "https://api.abalin.net/namedays"
    split_date = date.split("-", 3)
    month = int(split_date[1])
    day = int(split_date[2])
    payload = {'country': country, 'month': month, 'day': day}
    main_result = requests.get(endpoint, params=payload).json()['data']['namedays'][country]
    print(main_result)
    return render_template('namesday.html', names=main_result)


app.run(port=8080)
