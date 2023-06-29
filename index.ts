import express from "express";
import bodyParser from "body-parser";
import * as crypto from "crypto";
import * as dotenv from "dotenv"

const app = express();
//configure express
app.use(bodyParser.json());
//configure dotenv
dotenv.config()
//set port
const port = process.env.PORT || 3000;
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
  const hotp = truncated_hash %  Math.pow(10,8)
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
    const [user, password] = Buffer.from(auth_header.split(" ")[1], "base64")
      .toString()
      .split(":");
    //cek username apakah valid
    //cek otp nya apakah valid
    if (
      password !== generateOTP((process.env.SHARED_SECRET_BASE || "")+user)
    ) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  //akses aman
  res.status(201);
  res.setHeader("Content-Type", "text/plain; charset=UTF-8");
  res.send("Tes submit sukses! Silahkan submit berkas ke endpoint sebenarnya");
});
//buat submit bagian a
app.post("/submit/a", (req, res) => {
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
    //cek otp nya apakah valid
    if (
      password !== generateOTP((process.env.SHARED_SECRET_BASE || "")+user)
    ) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  //akses aman
  //TODO: simpan request
  res.status(201);
  res.setHeader("Content-Type", "text/plain; charset=UTF-8");
  res.send("Congratulations on completing part a!");
});
//buat submit bagian b
app.post("/submit/b", (req, res) => {
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
    //cek otp nya apakah valid
    if (
      password !== generateOTP((process.env.SHARED_SECRET_BASE || "")+user)
    ) {
      res.status(401).send("Unauthorized to access endpoint");
      return;
    }
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  //akses aman
  //TODO: simpan request
  res.status(201);
  res.setHeader("Content-Type", "text/plain; charset=UTF-8");
  res.send("Congratulations on completing part b!");
});

app.listen(port, () => {
  console.log(`App listening at port ${port}`);
});
