
PRAGMA foreign_keys;

DROP TABLE IF EXISTS user_treasure;
DROP TABLE IF EXISTS treasure;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id INTEGER PRIMARY KEY,    
    email TEXT NOT NULL UNIQUE,
    token TEXT NOT NULL,
    name TEXT NOT NULL,
    admin INTEGER CHECK (admin == 0 || admin == 1) DEFAULT 0,
    address TEXT 
);

CREATE TABLE treasure (
    id INTEGER PRIMARY KEY AUTOINCREMENT,    
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    description TEXT NOT NULL,
    userCreatorId INTEGER NOT NULL,
    challenge TEXT NOT NULL,
    challengeSolution TEXT NOT NULL, 
    FOREIGN KEY(userCreatorId) REFERENCES user(id)
);

CREATE TABLE user_treasure (
    userId INTEGER NOT NULL,
    treasureId INTEGER NOT NULL,
    PRIMARY KEY(userId,treasureId),
    FOREIGN KEY(userId) REFERENCES user(id),
    FOREIGN KEY(treasureId) REFERENCES treasure(id)
);

INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.177633, -8.595497,1883453695006163, 
    		  "Local de estudo ou de descanso? Míticos queijos!", 
    		  "Qual é o tipo de queijo mais consumido em Portugal?", 
    		  "Flamengo");
    		  
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.178128, -8.595272,1883453695006163, 
    		   "Tesouro de Engenharia Informática", 
    		   "Qual é a linguagem usada para desenvolver esta aplicação?", 
    		   "Java");
    		   
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.177826, -8.597467,1883453695006163, 
    		   "FEUP - Faculdade de Engenharia da Universidade do Porto", 
    		   "Em que ano foi fundada esta maravilhosa faculdade?", 
    		   "1926");
    		   
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.179144, -8.593946,1883453695006163, 
    		   "Tesouro do Parque dos Alunos", 
    		   "Qual o departamento mais próximo do parque dos alunos?", 
    		   "Mecânica");    
    		   
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.178209, -8.596396,1883453695006163, 
    		   "Tesouro de Materiais", 
    		   "Qual os seguintes materiais é mais duro? Seda de Teia de Aranha ou Aço?", 
    		   "Aço"); 	
    		   
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.178391, -8.597270,1883453695006163, 
    		   "Tesouro do Bar de Minas", 
    		   "Como se chama o senhor guru do troco do bar de minas?", 
    		   "José");   		   
    		   
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.176315, -8.595213,1883453695006163, 
    		   "Tesouro da Cantina", 
    		   "A maçã tem mais ou menos do que 50 calorias?", 
    		   "mais"); 
    		   
INSERT INTO treasure(latitude, longitude, userCreatorId,description,challenge, challengeSolution) 
    values(41.177815, -8.595243,1883453695006163, 
    		   "Tesouro do CICA", 
    		   "Qual o protocolo de VPN que o CICA utiliza para a VPN da FEUP?", 
    		   "l2tp");

INSERT INTO user(id, email, token, name,admin, address) 
    values(1972806272752741, "leogt-15@hotmail.com", "", "Leo Teixeira", 1, "");
