TableHighlighter = Class.create();
TableHighlighter.prototype = {

    initialize: function(id, decalx, decaly) {
        this.table = $(id);
        this.decalx = decalx;
        this.decaly = decaly;
        var trs = $$('#'+this.table.id+' tr');
        for (p=this.decaly;p<trs.length;++p){
            this.scan(trs[p]);
        }
    },

    scan: function(tr) {
        var element = $(tr);
        var descendants = element.getElementsByTagName('input');
        for(q=0;q<descendants.length;++q) {
            var td = $(descendants[q]);
            td.observe('mouseover', this.highlight.bind(this));
            td.observe('mouseout', this.highlight.bind(this));
        }
    },

    highlight: function(e) {
        var td = Event.element(e).parentNode;
        var tr = td.parentNode;
        var trs = $$('#'+this.table.id+' tr');
        var position = td.previousSiblings().length;

        for (p=this.decaly-1;p<trs.length;++p){
            var element = $(trs[p]);
            var num = position;
            if(p==1) num = num - this.decalx;
            element.immediateDescendants()[num ].toggleClassName('highlighted');
        }
        tr.toggleClassName('highlighted');
    }
};