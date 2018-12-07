(function(){

    function init(){
        var canvas = document.getElementById('background');
        var ctx = canvas.getContext('2d');
        ctx.canvas.width  = 1920;
        ctx.canvas.height = 1280;
        var container = {x:0,y:0,width:1920,height:1280};
        var img = [{x:760,y:440,vx:6,vy:10},
            {x:760,y:440,vx:2,vy:-8},
            {x:760,y:440,vx:5,vy:-2},
            {x:760,y:440,vx:-3,vy:-4},
            {x:760,y:440,vx:7,vy:-6},
            {x:760,y:440,vx:2,vy:-8},
            {x:760,y:440,vx:6,vy:-10},
            {x:760,y:440,vx:12,vy:2},
            {x:760,y:440,vx:10,vy:2},
            {x:760,y:440,vx:-8,vy:4},
            {x:760,y:440,vx:-8,vy:10},
            {x:760,y:440,vx:-10,vy:10},
        ];

        function draw(){
            ctx.fillStyle = '#939495';
            ctx.strokeStyle = 'white';
            ctx.fillRect(container.x,container.y,container.width,container.height);
            //ctx.clearRect(container.x,container.y,container.width,container.height);
            //ctx.strokeRect(container.x,container.y,container.width,container.height);

            for(var i=0; i <img.length; i++){
                // ctx.fillStyle = 'hsl(' + img[i].color + ',100%,50%)';

                ctx.globalAlpha = 0.6;
                var drawing = new Image(); // Using optional size for image
                if (i < 8)
                    drawing.src = 'img/img'+(i+1)+'.png';
                else drawing.src = 'img/img8.png';
                ctx.beginPath();
                if (i < 8)
                    ctx.drawImage(drawing,img[i].x,img[i].y);
                else ctx.drawImage(drawing,img[i].x,img[i].y, 200, 200);
                ctx.fill();

                if((img[i].x + img[i].vx  > container.x + container.width) || (img[i].x + img[i].vx < container.x)){
                    img[i].vx = - img[i].vx;
                }
                if((img[i].y + img[i].vy > container.y + container.height) || (img[i].y + img[i].vy < container.y)){
                    img[i].vy = - img[i].vy;
                }
                img[i].x +=img[i].vx;
                img[i].y +=img[i].vy;
            }
            requestAnimationFrame(draw);
        }
        requestAnimationFrame(draw);
    }
    //invoke function init once document is fully loaded
    window.addEventListener('load',init,false);

}());  //self invoking function
