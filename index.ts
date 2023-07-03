import express from "express";
import bodyParser from "body-parser";
import * as crypto from "crypto";
import * as dotenv from "dotenv";
import * as google from "googleapis";
import * as fs from "fs";
import { rejects } from "assert";
//
//fungsi buat authorization
// async function _getGoogleSheetClient() {
//   const auth = new google.Auth.GoogleAuth({
//     keyFile: serviceAccountKeyFile,
//     scopes: ['https://www.googleapis.com/auth/spreadsheets'],
//   });
//   const authClient = await auth.getClient();
//   // return google.sheets_v4({
//   //   version: 'v4',
//   //   auth: authClient,
//   // });
//   return new google.sheets_v4.Sheets()
// }
// let jwtClient = new google.Auth.JWT(secr)
interface SecretToken {
  type: string;
  project_id: string;
  private_key_id: string;
  private_key: string;
  client_email: string;
  client_id: string;
  auth_uri: string;
  token_uri: string;
  auth_provider_x509_cert_url: string;
  client_x509_cert_url: string;
  universe_domain: string;
}
interface SubmitData {
  NIM: string;
  fullName: string;
  link: string;
  message: string;
}
const app = express();
//configure express
app.use(bodyParser.json());
//configure dotenv
dotenv.config();
//set port
const port = process.env.PORT || 3000;
//SETUP buat google API
const serviceAccountKeyFile = `./credentials/${process.env.SERVICE_ACCOUNT_KEY_FILE}.json`;
const secretToken: SecretToken = JSON.parse(
  fs.readFileSync(serviceAccountKeyFile, "utf-8")
);
const jwtClient = new google.Auth.JWT(
  secretToken.client_email,
  undefined,
  secretToken.private_key,
  ["https://www.googleapis.com/auth/spreadsheets"]
);
jwtClient.authorize((err, token) => {
  if (err) {
    console.log(err);
    return;
  }
  console.log("Successfully connected");
});
const sheetsClient = google.google.sheets("v4");
const getData = (sheetRange: string) => {
  return new Promise((resolve, reject) => {
    let spreadsheetId = process.env.SHEET_ID;
    sheetsClient.spreadsheets.values.get(
      {
        auth: jwtClient,
        spreadsheetId: spreadsheetId,
        range: sheetRange,
      },
      (err, response) => {
        if (err) {
          console.log(`Failed to fetch data due to:\n${err}`);
          reject(err);
        } else {
          console.log("Successfully fetch data");
          // console.log(response?.data);
          resolve(response?.data);
        }
      }
    );
  });
};
const postData = async(data: SubmitData) => {
  let spreadsheetId = process.env.SHEET_ID;
  //dapetin row kosong pertama
  const firstEmptyRow: any = await getData("A:A");
  const emptyRowIndex = firstEmptyRow.values.length;

  const values = [
    [
      emptyRowIndex,
      data.NIM,
      data.fullName,
      data.link,
      data.message,
      new Date(),
    ],
  ];
  const sheetResources = { values };
  sheetsClient.spreadsheets.values.update({
    auth: jwtClient,
    spreadsheetId: spreadsheetId,
    range: `A${emptyRowIndex + 1}:F${emptyRowIndex + 1}`,
    requestBody: sheetResources,
    valueInputOption: "RAW",
  });
};

const isNIMSubmitted = async(NIM:string)=>{
  const NIMData: any = await getData("B:B");
  return NIMData && NIMData.values &&  NIMData.values.some((row:string[])=>row[0]==NIM)
}

//fungsi buat generate OTP
function generateOTP(secret: string, duration: number = 30) {
  //defaultnya 30 detik
  //set interval number
  const INTERVALS_NUMBER = Math.floor(Date.now() / (1000 * duration));
  //algoritma utama
  const msg = Buffer.alloc(8);
  msg.writeBigInt64BE(BigInt(INTERVALS_NUMBER));
  const hmac = crypto.createHmac("sha256", secret).update(msg).digest();
  const offset = hmac[hmac.length - 1] & 0xf;
  const truncated_hash =
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff);
  const hotp = truncated_hash % Math.pow(10, 8);
  //padding bila kurang dari 8 digit
  const otp = hotp.toString().padStart(8, "0");
  return otp;
}

//endpoint
//buat tes submit
app.post("/test", (req, res) => {
  //cek authorization header
  const auth_header = req.headers.authorization;
  if (!auth_header) {
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  //parse authorization
  try {
    //parse username dan password
    const [user, password]: string[] = Buffer.from(
      auth_header.split(" ")[1],
      "base64"
    )
      .toString()
      .split(":");
    //cek username apakah valid
    if (!user.startsWith("13521") && !user.startsWith("18221")) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
    //cek otp nya apakah valid
    if (
      password !== generateOTP((process.env.SHARED_SECRET_BASE || "") + user)
    ) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
    //cek apakah fullname dan linknya ada
    if (!req.body.fullName || !req.body.link) {
      res.status(400).send("Please fix payload");
      return;
    }
    //akses aman
    res.status(200);
    res.setHeader("Content-Type", "text/plain; charset=UTF-8");
    res.send(
      "Tes submit sukses! Silahkan submit berkas ke endpoint sebenarnya"
    );
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
});
//buat submit bagian a
app.post("/submit/a",async (req, res) => {
  //cek authorization header
  const auth_header = req.headers.authorization;
  if (!auth_header) {
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  //parse authorization
  try {
    //parse username dan password
    const [user, password] = Buffer.from(auth_header.split(" ")[1], "base64")
      .toString()
      .split(":");
    //cek username apakah valid
    if (!user.startsWith("13521") && !user.startsWith("18221")) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
    //cek otp nya apakah valid
    if (
      password !== generateOTP((process.env.SHARED_SECRET_BASE || "") + user)
    ) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
    //akses aman
    //cek apakah fullname dan linknya ada
    if (!req.body.fullName || !req.body.link) {
      res.status(400).send("Please fix payload");
      return;
    }
    //akses aman
    //cek apakah udah ada
    if(await isNIMSubmitted(user)){
      res.status(409).send("You already submitted response for part a")
      return;
    }
    //submit ke sheet
    const payload: SubmitData = {
      NIM: user,
      fullName: req.body.fullName,
      link: req.body.link,
      message: req.body.message,
    };
    await postData(payload);
    console.log(`${user} has submited part a`)
    res.status(201);
    res.setHeader("Content-Type", "text/plain; charset=UTF-8");
    res.send("Congratulations on completing part a!");
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
});
//buat submit bagian b
app.post("/submit/b", (req, res) => {
  //lock
  const LOCK_DATE = new Date(2023,6,22,7,0,0) //timezone:UTC , 6->indeks bulan (indeks 6=bulan 7), +7 ->buat handle UTC+7
  if(Date.now()<LOCK_DATE.getTime()){
    res.status(404).send('<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>Error</title>\n</head>\n<body>\n<pre>Cannot POST /submit/b</pre>\n</body>\n</html>\n')
    return;
  }
  //cek authorization header
  const auth_header = req.headers.authorization;
  if (!auth_header) {
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  //parse authorization
  try {
    //parse username dan password
    const [user, password] = Buffer.from(auth_header.split(" ")[1], "base64")
      .toString()
      .split(":");
    //cek username apakah valid
    if (!user.startsWith("13521") && !user.startsWith("18221")) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
    //cek otp nya apakah valid
    if (
      password !== generateOTP((process.env.SHARED_SECRET_BASE || "") + user)
    ) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
    //akses aman
    //cek apakah fullname dan linknya ada
    if (!req.body.fullName || !req.body.link) {
      res.status(400).send("Please fix payload");
      return;
    }
    //akses aman
    // getData('A1:F4')
    // const payload: SubmitData = {
    //   NIM: user,
    //   fullName: req.body.fullName,
    //   link: req.body.link,
    //   message: req.body.message,
    // };
    // postData(payload);
    res.status(201);
    res.setHeader("Content-Type", "text/plain; charset=UTF-8");
    res.send("Congratulations on completing part b!");
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
});

app.listen(port, () => {
  console.log(`App listening at port ${port}`);
});
