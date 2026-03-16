const mongoose = require('mongoose');

const router = require('express').Router();
const todo = require('../models/todo');

router.post('/add', async (req, res) => {
    let data = req.body;
    let todo = new todo(data);
    todo.save()
        .then((result) => {
            res.send(result);
        })
        .catch((err) => {
            res.send(err);
        });
    
});

router.get('/all', async (req, res) => {
    todo.find()
        .then((result) => {
            res.send(result);
        })
        .catch((err) => {
            res.send(err);
        });
  
});

router.delete('/delete/:id', async (req, res) => {
    let id = req.params.id;
    todo.findByIdAndDelete(id)
        .then((result) => {
            res.send(result);
        })
        .catch((err) => {
            res.send(err);
        });
});

module.exports = router;
