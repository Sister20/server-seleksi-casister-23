import express from "express";
import bodyParser from "body-parser";

const app = express();
//conigure express
app.use(bodyParser.json());
//set port
const port = process.env.PORT || 3000;

//endpoint
//buat tes submit
app.post("/test", (req, res) => {
  //cek authorization header
  const auth_header = req.headers.authorization;
  console.log(auth_header);
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
    console.log(user, password);
    //cek username apakah valid
  } catch (err) {
    console.log(err);
    res.status(401).send("Unauthorized to access endpoint");
    return;
  }
  res.status(201);
  res.setHeader("Content-Type", "text/plain; charset=UTF-8");
  res.send("Tes submit sukses");
});
//buat submit bagian a
app.post("/submit/a", (req, res) => {
  res.send("Congratulations on completing part a!");
});
//buat submit bagian b
app.post("/submit/b", (req, res) => {
  res.send("Congratulations on completing part b!");
});

app.listen(port, () => {
  console.log(`App listening at port ${port}`);
});
