new Vue({
    el: "#app",
    data: function () {
        return {
            extractInfo: {
                sentence: '',
                dims:[]
            },
            dimList: [
                {value: 'Age', label: '年龄'},
                {value: 'Area', label: '面积'},
                {value: 'BloodType', label: '血型'},
                {value: 'Constellation', label: '星座'},
                {value: 'Currency', label: '货币'},
                {value: 'Distance', label: '距离'},
                {value: 'Episode', label: 'Episode'},
                {value: 'Gender', label: '性别'},
                {value: 'Level', label: 'Level'},
                {value: 'Lyric', label: '歌词'},
                {value: 'Numeral', label: '数字'},
                {value: 'Fraction', label: '分数'},
                {value: 'DigitSequence', label: '数字序列'},
                {value: 'Ordinal', label: '序数'},
                {value: 'Place', label: '地点'},
                {value: 'Quantity', label: '单位'},
                {value: 'Velocity', label: '年龄'},
                {value: 'Rating', label: '评分'},
                {value: 'Season', label: 'Season'},
                {value: 'Temperature', label: '温度'},
                {value: 'Time', label: '时间'},
                {value: 'Date', label: '日期'},
                {value: 'Duration', label: '时间区间'},
                {value: 'URL', label: '链接'},
                {value: 'PhoneNumber', label: '电话'},
                {value: 'MultiChar', label: 'emoji'},
                {value: 'Duplicate', label: '重复串'},
                {value: 'Multiple', label: '倍数'}
            ],
            extractRes: ''
        };
    },

    methods: {
        extractDuckling: function () {
            var that = this;

            if (this.extractInfo.sentence.trim() == '') {
                that.$message({message: "待抽取文本的不能为空", type: 'warning'});
                return;
            }

            var serviceUrl = getExtractInfoUrl();
            let param = new URLSearchParams();
            param.append("sentence", this.extractInfo.sentence.trim());
            param.append("dims", this.extractInfo.dims.join(","));
            console.log('extractRequest is :' + param)

            axios.post(serviceUrl, param)
                .then(function (response) {
                    console.log(response);
                    that.$message({
                        message: "请求成功",
                        type: 'success'
                    });

                    that.extractRes = response.data.msg
            },
            err => {
                that.$message({message: "请求异常", type: 'warning'});
                console.log(err);
            });
        }
    }
});