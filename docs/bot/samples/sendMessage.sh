curl -X POST\
 -H "Content-Type: application/json"\
 -d '{"chat_id": 242892670, "parse_mode": "HTML", "text": "testing <b>parse</b> modes"}'\
 https://api.telegram.org/bot5223345298:AAGZ8JaU-kY5atE97EAVuAlX7ScuBXGSAEs/sendMessage

curl -X POST\
 -H "Content-Type: application/json"\
 -d '{"chat_id": 242892670, "text": "testing \nparse \nmodes"}'\
 https://api.telegram.org/bot5223345298:AAGZ8JaU-kY5atE97EAVuAlX7ScuBXGSAEs/sendMessage
