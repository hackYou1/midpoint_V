package com.evolveum.midpoint.prism.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WriteHTMLandCSSForDiagram {

    public void writeDocument(String path, String HTMLName, String jsName, String CSSName) throws IOException {
        File HTMLfile = new File(path + HTMLName);
        FileWriter HTMLwriter = new FileWriter(HTMLfile);
        HTMLwriter.write("<!DOCTYPE html>\n"
                + "<html>\n"
                + "    <head>\n"
                + "    <meta charset=\"utf-8\">\n"
                + "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width\">\n"
                + "\t<title> Connectors style example </title>\n"
                + "\t<link rel=\"stylesheet\" href=\"../../Treant.css\">\n"
                + "\t<link rel=\"stylesheet\" href=\"" + CSSName +"\">\n"
                + "\t\n"
                + "\t<link rel=\"stylesheet\" href=\"/home/jan/Downloads/treant-js-master/vendor/perfect-scrollbar/perfect-scrollbar.css\">\n"
                + "\t\n"
                + "</head>\n"
                + "<body>\n"
                + "\t<div class=\"chart\" id=\"hierarchy\"></div>\n"
                + "\t\n"
                + "\t<script src=\"../../vendor/jquery.min.js\"></script>\n"
                + "    \t<script src=\"../../vendor/jquery.easing.js\"></script>\n"
                + "\t\n"
                + "\t<script src=\"../../vendor/raphael.js\"></script>\n"
                + "\t<script src=\"../../Treant.js\"></script>\n"
                + "\t<script src=\"" + jsName + "\"></script>\n"
                + "\t<script src=\"/home/jan/node_modules/@panzoom/panzoom/dist/panzoom.js\"></script>\n"
                + "\n"
                + "\t<script>\n"
                + "\ttree = new Treant( chart_config );\n"
//                + "\tArray.from(document.getElementsByClassName('node')).forEach(function(currentValue, index) {\n" todo add later with support from RelDiagram class
//                + "    \t\tconsole.log(currentValue)\n"
//                + "    \t\tcurrentValue.addEventListener(\"mouseover\", function() {\n"
//                + "    \t\t\tvar propertyElements = currentValue.getElementsByClassName('properties')\n"
//                + "    \t\t\tif (propertyElements !== null) {\n"
//                + "    \t\t\t\tvar property = propertyElements[0]\n"
//                + "    \t\t\t\t//var charts = document.getElementsByClassName('chart')\n"
//                + "    \t\t\t\t//var chart = charts[0]\n"
//                + "    \t\t\t\tcurrentValue.style.zIndex = 100000\n"
//                + "    \t\t\t\tproperty.style.display = \"block\"\n"
//                + "    \t\t\t\t//chart.style.\n"
//                + "    \t\t\t}\n"
//                + "    \t\t});\n"
//                + "    \t\t\n"
//                + "    \t\tcurrentValue.addEventListener(\"mouseout\", function() {\n"
//                + "    \t\t\tconsole.log(\"halo\")\n"
//                + "    \t\t\tvar propertyElements = currentValue.getElementsByClassName('properties')\n"
//                + "    \t\t\tif (propertyElements !== null) {\n"
//                + "    \t\t\t\tvar property = propertyElements[0]\n"
//                + "    \t\t\t\tcurrentValue.style.zIndex = 1\n"
//                + "    \t\t\t\tproperty.style.display = \"none\"\n"
//                + "    \t\t\t}\n"
//                + "    \t\t});\n"
//                + "\t});\n"
                + "\tvar refs = document.getElementsByClassName('ref')\n"
                + "\tfor (var ref of refs) {\n"
                + "\t\tlet parentRef = ref.parentNode\n"
                + "\t\tconsole.log(parentRef.className)\n"
                + "\t\tparentRef.style.border = \"none\"\n"
                + "\t\tparentRef.style.boxShadow = \"none\"\n"
                + "\t\t//parentRef.style.minWidth = \"0px\"\n"
                + "\t}\n"
                + "\tconst elem = document.getElementById('hierarchy')\n"
                + "\tconst panzoom = Panzoom(elem, {\n"
                + "  \t\tmaxScale: 5\n"
                + "\t})\n"
                + "\tpanzoom.pan(10, 10)\n"
                + "\telem.addEventListener('wheel', panzoom.zoomWithWheel)\n"
                + "\t</script>\n"
                + "\n"
                + "</body>\n"
                + "</html>");

        File CSSfile = new File(path + CSSName);
        FileWriter CSSwriter = new FileWriter(CSSfile);
        CSSwriter.write("body,div,dl,dt,dd,ul,ol,li,h1,h2,h3,h4,h5,h6,pre,form,fieldset,input,textarea,p,blockquote,th,td { margin:0; padding:0; }\n"
                + "\n"
                + "\n"
                + "body { background: #fff; font-family: \"Droid Serif\",Georgia,\"Times New Roman\",Times,serif; color: #444444; overflow: visible}\n"
                + "\n"
                + "/* optional Container STYLES */\n"
                + ".chart { height: 100%; width: -moz-fit-content; width: fit-content; margin: 5px; margin: 15px auto; border: 3px solid #DDD; border-radius: 3px; overflow: visible}\n"
                + "\n"
                + ".node { font-size: 11px; }\n"
                + "\n"
                + "a {\n"
                + "\tcolor: black;\n"
                + "\ttext-decoration: none;\n"
                + "}\n"
                + "\n"
                + ".node.big-company {\n"
                + "\toverflow: visible\n"
                + "\tcursor: pointer;\n"
                + "\t/*padding: 5px;*/\n"
                + "\tpadding: 3px 0 4px 0;\n"
                + "\tmin-width: 60px;\n"
                + "\ttext-align: center;\n"
                + "\tborder: 1px solid #383838; \n"
                + "\tborder-radius: 2px;\n"
                + "\tlist-style-type: none;\n"
                + "\t/*background-color: rgb(252, 252, 252);*/\n"
                + "}\n"
                + "\n"
                + ".ref {\n"
                + "\tmargin: 2px;\n"
                + "\ttext-align: center;\n"
                + "\tborder: none;\n"
                + "\tvertical-align: middle;\n"
                + "\t/*line-height: 30px;*/\n"
                + "}\n"
                + "\n"
                + ".ref ul {\n"
                + "\tborder: none;\n"
                + "}\n"
                + "\n"
                + ".parentDef {\n"
                + "\tcolor: red;\n"
                + "}\n"
                + "\n"
                + ".title {\n"
                + "   align-items: center;\n"
                + "   /*margin: 5px;*/\n"
                + "}\n"
                + "\n"
                + ".properties ul, .propertiesExpanded ul {\n"
                + "\tborder: none;\n"
                + "\tpadding: 3px;\n"
                + "\n"
                + "\t/*margin: 4px;*/\n"
                + "}\n"
                + "\n"
                + "li {\n"
                + "   \n"
                + "}\n"
                + "\n"
                + "h1 {\n"
                + "   font-size: 9px;\n"
                + "}\n"
                + "\n"
                + "ul {\n"
                + "   list-style-type: none;\n"
                + "   border: 1px solid #383838;\n"
                + "   padding: 5px;\n"
                + "   margin-top: 3px;\n"
                + "   /*margin: 10px;*/\n"
                + "}\n"
                + "\n"
                + ".properties {\n"
                + "   display: none;\n"
                + "   /*border: 2px solid #000;\n"
                + "   padding: 3px;\n"
                + "   margin: 5px;*/\n"
                + "   text-align: left;\n"
                + "}\n"
                + "\n"
                + ".propertiesExpanded {\n"
                + "   display: block;\n"
                + "   /*border: 2px solid #000;\n"
                + "   margin: 5px;\n"
                + "   padding: 3px;*/\n"
                + "   text-align: left;\n"
                + "}\n"
                + "\n"
                + ".node.big-commpany:active { \n"
                + "\t/*box-shadow: inset 1px 1px 1px rgba(0,0,0,.1); \n"
                + "\tmargin: 1px 0 0 1px;*/ \n"
                + "\tborder: 2px solid #D3D3CB;\n"
                + "}\n"
                + "\n"
                + ".node.big-commpany .node-name {\n"
                + "\tline-height: 30px;\n"
                + "\tcolor: #9B9B9B;\n"
                + "}\n"
                + "\n"
                + ".node.big-commpany:hover .node-name {\n"
                + "\tcolor: #8B8B8B;\n"
                + "\ttext-shadow: 1px 1px rgba(0,0,0,.15);\n"
                + "}\n"
                + "\n"
                + ".Treant .collapse-switch { width: 100%; height: 100%; border: none;}\n"
                + ".Treant .node.collapsed { background-color: rgb(230, 230, 230); }\n"
                + ".Treant .node.collapsed .collapse-switch { background: none; }");
    }
}
