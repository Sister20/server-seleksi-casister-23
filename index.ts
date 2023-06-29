import express from "express"

const app = express()
const port = process.env.PORT || 3000

//endpoint
//buat tes submit
app.post("/test",(req,res)=>{
  res.send("Tes submit sukses")
})
//buat submit bagian a
app.post("/submit/a",(req,res)=>{
  res.send("Congratulations on completing part a!")
})
//buat submit bagian b
app.post("/submit/b",(req,res)=>{
  res.send("Congratulations on completing part b!")
})

app.listen(port,()=>{console.log(`App listening at port ${port}`)})